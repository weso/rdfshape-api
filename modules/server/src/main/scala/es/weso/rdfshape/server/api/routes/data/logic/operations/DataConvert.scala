package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.NONE
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.sgraph.{RDF2SGraph, RDFDotPreferences}
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  Dot,
  GraphicFormat,
  RdfFormat,
  Json => JsonFormat
}
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataConvert.successMessage
import es.weso.rdfshape.server.api.routes.data.logic.types.{Data, DataSingle}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import scala.collection.immutable

/** Data class representing the output of a data-conversion operation
  *
  * @param inputData    Data before conversion
  * @param targetFormat Target data format
  * @param result       Data after conversion
  */
final case class DataConvert private (
    override val inputData: Data,
    targetFormat: DataFormat,
    result: Data
) extends DataOperation(successMessage, inputData)

/** Static utilities for data conversion
  */
private[api] object DataConvert extends LazyLogging {

  /** List of available RDF format names (uppercase)
    */
  private lazy val rdfDataFormatNames: immutable.Seq[String] =
    RDFAsJenaModel.availableFormats.map(_.toUpperCase)

  /** Convert a [[DataConvert]] to its JSON representation
    *
    * @return JSON representation of the conversion result
    */
  implicit val encodeDataConversionOperation: Encoder[DataConvert] =
    (dataConversion: DataConvert) => {
      Json.fromFields(
        List(
          ("message", Json.fromString(dataConversion.successMessage)),
          ("data", dataConversion.inputData.asJson),
          ("result", dataConversion.result.asJson),
          ("targetDataFormat", dataConversion.result.format.asJson)
        )
      )
    }
  private val successMessage = "Conversion successful"

  /** Perform the actual conversion operation between RDF text formats
    *
    * @param inputData    Input conversion data
    * @param targetFormat Target format
    * @return A new [[DataConvert]] instance with the conversion information
    */
  def dataConvert(
      inputData: Data,
      targetFormat: DataFormat
  ): IO[DataConvert] = {
    logger.info(s"Data conversion target format: ${targetFormat.name}")
    for {
      // Get a handle to the RDF resource
      rdf <- inputData.toRdf()
      // Compute the inference to be used
      targetInference = inputData match {
        case ds: DataSingle => ds.inference
        case _              => NONE
      }

      // Perform the conversion while using the RDF resource
      conversionResult <- rdf.use(rdfReasoner => {
        for {
          sgraph <- RDF2SGraph.rdf2sgraph(rdfReasoner)
          convertedData <- targetFormat match {
            // JSON: convert to JSON String and return a DataSingle with it
            case JsonFormat =>
              IO {
                DataSingle(
                  content = sgraph.toJson.spaces2,
                  format = JsonFormat,
                  inference = targetInference
                )
              }

            case Dot =>
              IO {
                DataSingle(
                  content = sgraph.toDot(RDFDotPreferences.defaultRDFPrefs),
                  format = Dot,
                  inference = targetInference
                )
              }
            case _ if RdfFormat.availableFormats.contains(targetFormat) =>
              rdfReasoner
                .serialize(targetFormat.name)
                .map(data => {
                  DataSingle(
                    content = data,
                    format = targetFormat,
                    inference = targetInference
                  )
                })
            case _ if GraphicFormat.availableFormats.contains(targetFormat) =>
              IO {
                DataSingle(
                  content = sgraph.toDot(RDFDotPreferences.defaultRDFPrefs),
                  format = targetFormat,
                  inference = targetInference
                )
              }
            case t =>
              IO.raiseError(new RuntimeException(s"Unsupported format: $t"))
          }
        } yield DataConvert(inputData, targetFormat, convertedData)
      })
    } yield conversionResult
  }
}
