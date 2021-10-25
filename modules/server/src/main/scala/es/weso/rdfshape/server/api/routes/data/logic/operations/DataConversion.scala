package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.sgraph.{RDF2SGraph, RDFDotPreferences}
import es.weso.rdf.{InferenceEngine, NONE}
import es.weso.rdfshape.server.api.format.dataFormats.{DataFormat, Png, Svg}
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataConversion.successMessage
import es.weso.rdfshape.server.api.routes.data.logic.types.{Data, DataSingle}
import es.weso.utils.IOUtils.either2io
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import scala.collection.immutable
import scala.util.Try

/** Data class representing the output of a data-conversion operation
  *
  * @param inputData    Data before conversion
  * @param targetFormat Target data format
  * @param result       Data after conversion
  */
final case class DataConversion private (
    override val inputData: Data,
    targetFormat: DataFormat,
    result: Data
) extends DataOperation(successMessage, inputData)

/** Static utilities for data conversion
  */
private[api] object DataConversion extends LazyLogging {

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

  /** Convert a conversion result to its JSON representation
    *
    * @return JSON representation of the conversion result
    */
  implicit val encodeResult: Encoder[DataConversion] =
    (dataConversion: DataConversion) => {
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
  /* TODO: weird NullPointerException on data merges when using the "rdf"
   * resource */

  def dataConvert(
      inputData: Data,
      targetFormat: DataFormat
  ): IO[DataConversion] = {
    logger.info(s"Conversion target format: $targetFormat")
    for {
      // Get a handle to the RDF resource
      rdf <- inputData.toRdf()
      _   <- IO.println("STATS")
      _   <- IO.println(inputData.getClass)
      _   <- IO.println(inputData.format)
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
                  dataRaw = sgraph.toJson.spaces2,
                  dataFormat = targetFormat,
                  inference = targetInference,
                  activeDataSource = inputData.dataSource
                )
              }

            case "DOT" =>
              IO {
                DataSingle(
                  dataRaw = sgraph.toDot(RDFDotPreferences.defaultRDFPrefs),
                  dataFormat = targetFormat,
                  inference = targetInference,
                  activeDataSource = inputData.dataSource
                )
              }
            case tFormat if rdfDataFormatNames.contains(tFormat) =>
              for {
                data <- rdfReasoner.serialize(tFormat)
              } yield DataSingle(
                dataRaw = data,
                dataFormat = targetFormat,
                inference = targetInference,
                activeDataSource = inputData.dataSource
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
                dataRaw = data,
                dataFormat = targetFormat,
                inference = targetInference,
                activeDataSource = inputData.dataSource
              )
            case t =>
              IO.raiseError(new RuntimeException(s"Unsupported format: $t"))
          }
        } yield DataConversion(inputData, targetFormat, convertedData)
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

  /** Perform a conversion from DOT data to another format
    *
    * @param inputData    Input Data (DOT format) to be converted
    * @param targetFormat Target format (graphviz)
    * @return Data after conversion
    */
  private def dotConvert(
      inputData: Data,
      targetFormat: Format,
      inference: InferenceEngine = NONE
  ): IO[Data] = {
    logger.debug(s"dotConverter to $targetFormat. dot\n$inputData")
    if(inputData.format.isEmpty)
      IO.raiseError(new RuntimeException("Unspecified input data format"))
    else if(inputData.rawData.isEmpty)
      IO.raiseError(
        new RuntimeException("Empty or malformed input data contents")
      )
    else if(
      inputData.format.get != es.weso.rdfshape.server.api.format.dataFormats.Dot
    ) IO.raiseError(new RuntimeException("Input format is not DOT"))
    else {
      Try {
        val g: MutableGraph = new Parser().read(inputData.rawData.get)
        targetFormat match {
          case Format.SVG =>
            val renderer = Graphviz
              .fromGraph(g) //.width(200)
              .render(targetFormat)
            logger.debug(s"SVG converted: ${renderer.toString}")
            IO {
              DataSingle(
                dataRaw = renderer.toString,
                dataFormat = Svg,
                inference = inference,
                activeDataSource = inputData.dataSource
              )
            }
          case Format.PNG =>
            val renderer = Graphviz.fromGraph(g).render(Format.PNG)
            val image    = renderer.toImage
            val baos     = new ByteArrayOutputStream()
            ImageIO.write(image, "png", baos)
            val data        = Base64.getEncoder.encodeToString(baos.toByteArray)
            val imageString = "data:image/png;base64," + data

            IO {
              DataSingle(
                dataRaw =
                  "<html><body><img src='" + imageString + "'></body></html>",
                dataFormat = Png,
                inference = inference,
                activeDataSource = inputData.dataSource
              )
            }

          case _ =>
            IO.raiseError(
              new RuntimeException(
                s"Error converting from DOT to $targetFormat"
              )
            )
        }
      }.fold(
        err => IO.raiseError(err),
        identity
      )
    }
  }

  private case class GraphFormat(name: String, mime: String, fmt: Format)
}
