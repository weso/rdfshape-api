package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.NONE
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.sgraph.{RDF2SGraph, RDFDotPreferences}
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataConvert.successMessage
import es.weso.rdfshape.server.api.routes.data.logic.types.{Data, DataSingle}
import es.weso.utils.IOUtils.either2io
import guru.nidi.graphviz.engine.Format
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

  /** List of graph format names
    */
  private lazy val availableGraphFormatNames: immutable.Seq[String] =
    availableGraphFormats.map(_.name)

  /** List of available RDF format names (uppercase)
    */
  private lazy val rdfDataFormatNames: immutable.Seq[String] =
    RDFAsJenaModel.availableFormats.map(_.toUpperCase)

  /** List of available graph formats
    */
  private lazy val availableGraphFormats = List(
    GraphFormat("SVG", "application/svg", Format.SVG),
    GraphFormat("PNG", "application/png", Format.PNG),
    GraphFormat("PS", "application/ps", Format.PS)
  )

  private val successMessage = "Conversion successful"

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
          ("inputDataFormat", dataConversion.inputData.format.asJson),
          ("targetDataFormat", dataConversion.result.format.asJson)
        )
      )
    }

  /** Perform the actual conversion operation between RDF text formats
    *
    * @param inputData    Input conversion data
    * @param targetFormat Target
    * @return A new Data instance
    */
  def dataConvert(
      inputData: Data,
      targetFormat: DataFormat
  ): IO[DataConvert] = {
    logger.info(s"Conversion target format: $targetFormat")
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
          convertedData <- targetFormat.name.toUpperCase match {
            // JSON: convert to JSON String and return a DataSingle with it
            case "JSON" =>
              IO {
                DataSingle(
                  dataPre = Option(sgraph.toJson.spaces2),
                  dataFormat = targetFormat,
                  inference = targetInference,
                  dataSource = inputData.dataSource
                )
              }

            case "DOT" =>
              IO {
                DataSingle(
                  dataPre =
                    Option(sgraph.toDot(RDFDotPreferences.defaultRDFPrefs)),
                  dataFormat = targetFormat,
                  inference = targetInference,
                  dataSource = inputData.dataSource
                )
              }
            case tFormat if rdfDataFormatNames.contains(tFormat) =>
              for {
                data <- rdfReasoner.serialize(tFormat)
              } yield DataSingle(
                dataPre = Option(data),
                dataFormat = targetFormat,
                inference = targetInference,
                dataSource = inputData.dataSource
              )
            case tFormat if availableGraphFormatNames.contains(tFormat) =>
              for {
                eitherFormat <- either2io(getTargetFormat(tFormat))
                dotStr = sgraph.toDot(RDFDotPreferences.defaultRDFPrefs)
                data <- eitherFormat.fold(
                  err => IO.raiseError(new RuntimeException(err)),
                  _ => IO(dotStr)
                )
              } yield DataSingle(
                dataPre = Option(data),
                dataFormat = targetFormat,
                inference = targetInference,
                dataSource = inputData.dataSource
              )
            case t =>
              IO.raiseError(new RuntimeException(s"Unsupported format: $t"))
          }
        } yield DataConvert(inputData, targetFormat, convertedData)
      })
    } yield conversionResult
  }

  private def getTargetFormat(str: String): Either[String, Format] =
    str.toUpperCase match {
      case "SVG" => Right(Format.SVG)
      case "PNG" => Right(Format.PNG)
      case "PS"  => Right(Format.PS)
      case _     => Left(s"Unsupported format $str")
    }

  private case class GraphFormat(name: String, mime: String, fmt: Format)
}
