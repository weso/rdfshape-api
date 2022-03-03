package es.weso.rdfshape.server.api.routes.data.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefaults.defaultInferenceEngine
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.availableInferenceEngines
import es.weso.rdfshape.server.api.format.dataFormats._
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.ShExC
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.operations.{
  DataConvert,
  DataExtract,
  DataInfo,
  DataQuery
}
import es.weso.rdfshape.server.api.routes.data.service.operations.{
  DataConvertInput,
  DataExtractInput,
  DataInfoInput,
  DataQueryInput
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.schema.ShExSchema
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._

/** API Service to handle RDF data
  *
  * @param client HTTP4S client object
  */
//noinspection DuplicatedCode
class DataService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "data"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    "Get an array with the accepted input RDF data formats" **
      GET / `verb` / "formats" / "input" |>> {
        val formats     = RdfFormat.availableFormats ++ HtmlFormat.availableFormats
        val formatNames = formats.map(_.name)
        val json        = Json.fromValues(formatNames.map(Json.fromString))
        Ok(json)
      }

    "Get an array with the available output RDF data formats" **
      GET / `verb` / "formats" / "output" |>> {
        val formatNames =
          RdfFormat.availableFormats.filter(!_.equals(Mixed)).map(_.name)
        val json = Json.fromValues(formatNames.map(Json.fromString))
        Ok(json)
      }

    "Get an array with the available visualization formats" **
      GET / `verb` / "formats" / "visual" |>> {
        val formats = GraphicFormat.availableFormats.map(_.name)
        val json    = Json.fromValues(formats.map(Json.fromString))
        Ok(json)
      }

    "Get the default RDF format as raw text" **
      GET / `verb` / "formats" / "default" |>> {
        Ok(DataFormat.default.name)
      }

    "Get an array with the available inference engines" **
      GET / `verb` / "inferenceEngines" |>> {
        val inferenceEngines = availableInferenceEngines
        val json =
          Json.fromValues(inferenceEngines.map(e => Json.fromString(e.name)))
        Ok(json)
      }

    "Get the default inference engine used as raw text" **
      GET / `verb` / "inferenceEngines" / "default" |>> {
        Ok(defaultInferenceEngine.name)
      }

    s"""Get an array with the valid data sources that the server will
       | accept when sent via '${SourceParameter.name}' parameter""".stripMargin **
      GET / `verb` / "sources" |>> {
        val json = Json.arr(
          Json.fromString(DataSource.TEXT),
          Json.fromString(DataSource.URL),
          Json.fromString(DataSource.FILE)
        )
        Ok(json)
      }

    s"""Obtain information about an RDF data instance: 
       | number of statements, prefix map and predicates
       |""".stripMargin **
      POST / `verb` / "info" ^ jsonOf[IO, DataInfoInput] |>> {
        body: DataInfoInput =>
          DataInfo
            .dataInfo(body.data)
            .flatMap(info => Ok(info.asJson))
            .handleErrorWith(err => {
              // Legacy code may return exceptions with "null" messages
              val errorMessage =
                if(err.getMessage != null) err.getMessage
                else DataServiceError.couldNotParseData
              InternalServerError(errorMessage)
            })
      }

    "Convert RDF data into another formats" **
      POST / `verb` / "convert" ^ jsonOf[IO, DataConvertInput] |>> {
        body: DataConvertInput =>
          DataConvert
            .dataConvert(body.data, body.targetFormat)
            .flatMap(conversion => Ok(conversion.asJson))
            .handleErrorWith(err => {
              // Legacy code may return exceptions with "null" messages
              val errorMessage =
                if(err.getMessage != null) err.getMessage
                else DataServiceError.couldNotParseData
              InternalServerError(errorMessage)
            })
      }

    "Perform a SPARQL query on RDF data" **
      POST / `verb` / "query" ^ jsonOf[IO, DataQueryInput] |>> {
        body: DataQueryInput =>
          DataQuery
            .dataQuery(body.data, body.query)
            .flatMap(result => Ok(result.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Attempt to extract a ShEx schema from an RDF source" **
      POST / `verb` / "extract" ^ jsonOf[IO, DataExtractInput] |>> {
        body: DataExtractInput =>
          DataExtract
            .dataExtract(
              body.data,
              body.nodeSelector,
              Option(ShExSchema.empty),
              Option(ShExC),
              body.label,
              relativeBase = None
            )
            .flatMap(result => Ok(result.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))
      }
  }

}

object DataService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Data Service
    */
  def apply(client: Client[IO]): DataService =
    new DataService(client)
}

/** Compendium of additional text constants used on service errors
  */
private object DataServiceError extends Enumeration {
  type DataServiceError = String
  val couldNotParseData: DataServiceError =
    "Unknown error parsing the data provided. Check the input and the selected format."

  val noNodeSelector: DataServiceError =
    "No node selector provided for extraction."

  val emptyNodeSelector: DataServiceError =
    "Empty node selector provided for extraction."
}
