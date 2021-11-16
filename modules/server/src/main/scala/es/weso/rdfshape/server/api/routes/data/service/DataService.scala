package es.weso.rdfshape.server.api.routes.data.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefaults.{
  availableInferenceEngines,
  defaultInferenceEngineName
}
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
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
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.schema.ShExSchema
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart

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
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Returns a JSON array with the accepted input or output RDF data formats
      */
    case GET -> Root / `api` / `verb` / "formats" / "input" =>
      val formats     = RDFFormat.availableFormats ++ HtmlFormat.availableFormats
      val formatNames = formats.map(_.name)
      val json        = Json.fromValues(formatNames.map(Json.fromString))
      Ok(json)

    /** Returns a JSON array with the available output RDF data formats
      */
    case GET -> Root / `api` / `verb` / "formats" / "output" =>
      val formatNames = RDFFormat.availableFormats.map(_.name)
      val json        = Json.fromValues(formatNames.map(Json.fromString))
      Ok(json)

    /** Returns a JSON array with the available visualization formats
      */
    case GET -> Root / `api` / `verb` / "formats" / "visual" =>
      val formats = GraphicFormat.availableFormats.map(_.name)
      val json    = Json.fromValues(formats.map(Json.fromString))
      Ok(json)

    /** Returns the default RDF format as a raw string
      */
    case GET -> Root / `api` / `verb` / "formats" / "default" =>
      val dataFormat = DataFormat.defaultFormat.name
      Ok(Json.fromString(dataFormat))

    /** Returns a JSON array with the available inference engines
      */
    case GET -> Root / `api` / `verb` / "inferenceEngines" =>
      val inferenceEngines = availableInferenceEngines
      val json             = Json.fromValues(inferenceEngines.map(Json.fromString))
      Ok(json)

    /** Returns the default inference engine used as a raw string
      */
    case GET -> Root / `api` / `verb` / "inferenceEngines" / "default" =>
      val defaultInferenceEngine = defaultInferenceEngineName
      Ok(Json.fromString(defaultInferenceEngine))

    /** Returns a JSON array with the valid data sources that the server will accept when sent via [[DataSourceParameter]]
      */
    case GET -> Root / `api` / `verb` / "sources" =>
      val json = Json.arr(
        Json.fromString(DataSource.TEXT),
        Json.fromString(DataSource.URL),
        Json.fromString(DataSource.FILE)
      )
      Ok(json)

    /** Obtain information about an RDF source.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: RDF data (raw, URL containing the data or File with the data)
      *  - dataSource [String]: Identifies the source of the data (raw, URL, file...) so that the server knows how to handle it
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *    Returns a JSON object with the operation results. See [[DataInfo.encodeDataInfoOperation]]
      */
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        for {
          // Get the data from the partsMap
          eitherData <- Data.mkData(partsMap)
          response <- eitherData.fold(
            // If there was an error parsing the data, return it
            err => errorResponseJson(err, InternalServerError),
            // Else, try and compute the data info
            data =>
              DataInfo
                .dataInfo(data)
                .flatMap(info => Ok(info.asJson))
                .handleErrorWith(err =>
                  // Legacy code may return exceptions with "null" messages
                  err.getMessage match {
                    case errorMessage: String =>
                      errorResponseJson(errorMessage, InternalServerError)
                    case _ => // null exception message, return a general error message
                      errorResponseJson(
                        DataServiceError.couldNotParseData,
                        InternalServerError
                      )
                  }
                )
          )
        } yield response
      }

    /** Convert an RDF source into another format/syntax.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: RDF data (raw, URL containing the data or File with the data)
      *  - dataSource [String]: Identifies the source of the data (raw, URL, file...) so that the server knows how to handle it
      *  - targetDataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *    Returns a JSON object with the operation results. See [[DataConvert.encodeDataConversionOperation]].
      */
    case req @ POST -> Root / `api` / `verb` / "convert" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        for {
          // Get the data from the partsMap
          eitherData <- Data.mkData(partsMap)
          // Get the target data format
          optTargetFormatStr <- partsMap.optPartValue(
            TargetDataFormatParameter.name
          )

          optTargetFormat = for {
            targetFormatStr <- optTargetFormatStr
            // Standard data format or graphical format
            targetFormat <- DataFormat
              .fromString(targetFormatStr)
              .toOption
          } yield targetFormat

          // Abort if no valid target format, else continue
          response <- optTargetFormat match {
            case None =>
              errorResponseJson(
                "Empty or invalid target format for conversion",
                BadRequest
              )
            case Some(targetFormat) =>
              eitherData.fold(
                // If there was an error parsing the data, return it
                err => errorResponseJson(err, InternalServerError),
                // Else, try and compute the data conversion
                data =>
                  // Check for exceptions when converting the data
                  DataConvert
                    .dataConvert(data, targetFormat)
                    .flatMap(conversion => Ok(conversion.asJson))
                    .handleErrorWith(err =>
                      err.getMessage match {
                        case errorMessage: String =>
                          errorResponseJson(errorMessage, InternalServerError)
                        case _ => // null exception message, return a general error message
                          err.printStackTrace()
                          errorResponseJson(
                            DataServiceError.couldNotParseData,
                            InternalServerError
                          )
                      }
                    )
              )
          }
        } yield response
      }

    /** Perform a SPARQL query on RDF data.
      * Receives a JSON object with the input RDF and query information:
      *  - data [String]: RDF data (raw, URL containing the data or File with the data)
      *  - dataSource [String]: Identifies the source of the data (raw, URL, file...) so that the server knows how to handle it
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *
      *  - query [String]: SPARQL query data (raw, URL containing the data or File with the query)
      *  - querySource [String]: Identifies the source of the query (raw, URL, file...) so that the server knows how to handle it
      *
      * Returns a JSON object with the query inputs and results (see [[DataQuery.encodeDataQueryOperation]]).
      */
    case req @ POST -> Root / `api` / `verb` / "query" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get the data from the partsMap
          eitherData <- Data.mkData(partsMap)
          // Get the query from the partsMap
          eitherQuery <- SparqlQuery.mkSparqlQuery(partsMap)

          /* Accumulate either:
           * - the errors occurred parsing the data/query
           * - the results of parsing the data/query in a single value */
          eitherInputs: Either[String, (Data, SparqlQuery)] = for {
            data  <- eitherData
            query <- eitherQuery
          } yield (data, query)

          // Make response
          response <- eitherInputs.fold(
            // If there was an error parsing the data/query, return it
            err => errorResponseJson(err, InternalServerError),
            // Else, try and compute the query, first destructuring the tuple
            {
              // Destructure tuple
              case (data, query) =>
                DataQuery
                  .dataQuery(data, query)
                  .flatMap(result => Ok(result.asJson))
                  .handleErrorWith(err =>
                    errorResponseJson(err.getMessage, InternalServerError)
                  )

              // Generic error. Code should not reach here.
              case _ =>
                errorResponseJson(
                  DataServiceError.couldNotParseData,
                  InternalServerError
                )
            }
          )
        } yield response
      }

    /** Attempt to extract a schema from an RDF source.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: RDF data (raw, URL containing the data or File with the data)
      *  - dataSource [String]: Identifies the source of the data (raw, URL, file...) so that the server knows how to handle it
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - nodeSelector [String]: Node selector to use
      *    Returns a JSON object with the extraction information (see [[DataExtract.encodeDataExtractOperation]]
      */
    case req @ POST -> Root / `api` / `verb` / "extract" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get the data from the partsMap
          eitherData <- Data.mkData(partsMap)
          // Schema format and engine will be ShEx, force later.
          // Try to map label to IRI or node selector
          optLabel <- partsMap
            .optPartValue(LabelParameter.name)
            .map(_.map(IRI(_)))
          // Try to get node selector
          optNodeSelectorStr <- partsMap.optPartValue(
            NodeSelectorParameter.name
          )

          response <- eitherData.fold(
            // If there was an error parsing the data, return it
            err => errorResponseJson(err, InternalServerError),
            // Else, try and compute the shex extraction
            data =>
              // Return error if no node selector
              optNodeSelectorStr match {
                case None =>
                  errorResponseJson(DataServiceError.noNodeSelector, BadRequest)
                case Some(nodeSelector) if nodeSelector.isBlank =>
                  errorResponseJson(
                    DataServiceError.emptyNodeSelector,
                    BadRequest
                  )
                case Some(nodeSelector) =>
                  DataExtract
                    .dataExtract(
                      data,
                      nodeSelector,
                      Option(ShExSchema.empty),
                      Option(ShExC),
                      optLabel,
                      relativeBase = None
                    )
                    .flatMap(result => Ok(result.asJson))
                    .handleErrorWith(err =>
                      errorResponseJson(err.getMessage, InternalServerError)
                    )
              }
          )

        } yield response

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

private object DataServiceError extends Enumeration {
  type DataServiceError = String
  val couldNotParseData: DataServiceError =
    "Unknown error parsing the data provided. Check the input and the selected format."

  val noNodeSelector: DataServiceError =
    "No node selector provided for extraction."

  val emptyNodeSelector: DataServiceError =
    "Empty node selector provided for extraction."
}
