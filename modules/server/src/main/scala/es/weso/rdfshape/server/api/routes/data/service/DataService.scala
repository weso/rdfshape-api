package es.weso.rdfshape.server.api.routes.data.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.definitions.ApiDefaults.{
  availableInferenceEngines,
  defaultInferenceEngineName
}
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  GraphicFormat,
  RDFFormat
}
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.data.logic.operations.{
  DataConversion,
  DataInfo
}
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
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
      val formatNames = RDFFormat.availableFormats.map(_.name)
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

    /** Obtain information about an RDF source.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: RDF data
      *  - dataUrl [String]: Url containing the RDF data
      *  - dataFile [File Object]: File containing RDF data
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - activeDataSource [String]: Identifies the source of the data (raw, URL, file...)
      *    Returns a JSON object with the operation results. See [[DataInfo.encodeResult]]
      */
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get the data from the partsMap
          eitherData <- Data.mkData(partsMap)
          response <- eitherData.fold(
            // If there was an error, return it
            err => errorResponseJson(err, InternalServerError),
            // Else, try and compute the data info
            data =>
              for {
                // Check for exceptions when getting the data info
                maybeResult <- DataInfo.dataInfo(data).attempt
                response <- maybeResult.fold(
                  // Error: return it
                  err =>
                    // Legacy code may return exceptions with "null" messages
                    err.getMessage match {
                      case errorMessage: String =>
                        errorResponseJson(errorMessage, InternalServerError)
                      case _ => // null exception message, return a general error message
                        errorResponseJson(
                          DataServiceError.couldNotParseData,
                          InternalServerError
                        )
                    },
                  // Success: build successful response
                  dataInfo => Ok(dataInfo.asJson)
                )
              } yield response
          )
        } yield response
      }

    /** Convert an RDF source into another format/syntax.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: RDF data
      *  - dataUrl [String]: Url containing the RDF data
      *  - dataFile [File Object]: File containing RDF data
      *  - dataFormat [String]: Format of the RDF data
      *  - targetDataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - activeDataSource [String]: Identifies the source of the data (raw, URL, file...)
      *    Returns a JSON object with the operation results. See [[DataConversion.encodeResult]]:
      *    - message [String]: Informational message
      *    - data [String]: RDF data sent back (originally sent by the client)
      *    - inputDataFormat [String]: Data format of the input data
      *    - targetDataFormat [String]: Data format of the output data
      *    - result[Object]: JSON representation of the resulting data. See [[Data.encodeData]]
      */
    //    case req @ POST -> Root / `api` / `verb` / "convert" =>
    //      req.decode[Multipart[IO]] { m =>
    //        val partsMap = PartsMap(m.parts)
    //        for {
    //          dataParam <- DataSingle.getData(partsMap, relativeBase)
    //          (resourceRdf, dp) = dataParam
    /* targetFormat = dp.targetDataFormat.getOrElse(defaultDataFormat).name */
    /* dataFormat = dp.optDataFormat.getOrElse(defaultDataFormat) */
    //
    //          result <- io2f(
    //            resourceRdf.use(rdf => {
    //              logger.debug(s"Attempting data conversion")
    //              DataConversion
    //                .dataConvert(rdf, dp.data, dataFormat, targetFormat)
    //
    //            })
    //          ).attempt
    //            .map(
    //              _.fold(exc => Left(exc.getMessage), dc => Right(dc))
    //            )
    //
    //          response <- result match {
    /* case Left(err) => errorResponseJson(err, InternalServerError) */
    //            case Right(result) => Ok(result.toJson)
    //          }
    //
    //        } yield response
    //      }

    /** Perform a SPARQL query on RDF data.
      * Receives a JSON object with the input RDF and query information:
      *  - data [String]: Raw RDF data
      *  - dataUrl [String]: Url containing the RDF data
      *  - dataFile [File Object]: File containing RDF data
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - query [String]: Raw SPARQL query
      *  - queryUrl [String]: Url containing the SPARQL query
      *  - queryFile [String]: File containing the SPARQL query
      *  - activeDataSource [String]: Identifies the source of the data (raw, URL, file...)
      *  - activeQueryTab [String]: Identifies the source of the query (raw, URL, file...)
      *    Returns a JSON object with the RDF data information:
      *    - message [String]: Informational message
      *    - data [String]: RDF data sent back (originally sent by the client)
      *    - result [String]: RDF resulting from the conversion
      *    - dataFormat [String]: Data format of the input data
      *    - targetDataFormat [String]: Data format of the output data
      *    - numberOfStatements [String]: Data format of the data
      *    - prefixMap [Object]: Dictionary with the prefix map of the data
      *    - predicates [Array]: Array of the predicates present in the data
      */
    //    case req @ POST -> Root / `api` / `verb` / "query" =>
    //      req.decode[Multipart[IO]] { m =>
    //        val partsMap = PartsMap(m.parts)
    //        logger.debug(s"Data query params map: $partsMap")
    //        for {
    /* /* TODO: an error is thrown on bad query URLs (IO.raise...), but it is */
    //           * not controlled */
    //          dataParam <- DataSingle.getData(partsMap, relativeBase)
    //
    //          (resourceRdf, dp) = dataParam
    //          maybeQuery <- SparqlQuery.getSparqlQuery(partsMap)
    //          resp <- maybeQuery match {
    //            case Left(err) =>
    //              // Query could not be even parsed from user data
    /* errorResponseJson(s"Error obtaining query data: $err", BadRequest) */
    //            case Right(query) =>
    //              // Query was parsed, but may be invalid still
    //              val optQueryStr = query.queryRaw
    //              logger.debug(s"Data query with querystring: $optQueryStr")
    //              for {
    //                result <- io2f(
    //                  resourceRdf.use(rdf => rdf.queryAsJson(optQueryStr))
    //                ).attempt
    /* .map(_.fold(exc => Left(exc.getMessage), dc => Right(dc))) */
    //                response <- result match {
    /* case Left(err) => errorResponseJson(err, InternalServerError) */
    //                  case Right(json) => Ok(json)
    //                }
    //              } yield response
    //          }
    //        } yield resp
    //      }

    /** Attempt to extract a schema from an RDF source.
      * Receives a JSON object with the input RDF information:
      *  - data [String]: Raw RDF data
      *  - dataUrl [String]: Url containing the RDF data
      *  - dataFile [File Object]: File containing RDF data
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - activeDataSource [String]: Identifies the source of the data (raw, URL, file...)
      *    Returns a JSON object with the extraction information:
      *    - message [String]: Informational message
      *    - data [String]: Input RDF data
      *    - dataFormat [String]: Format of the input RDF data
      *    - inferredShape [String]: Raw extracted shape
      *    - schemaFormat [String]: Format of the extracted schema
      *    - schemaEngine [String]: Engine of the extracted schema
      *    - resultShapeMap [String]: Shapemap of the extracted schema
      */
    //    case req @ POST -> Root / `api` / `verb` / "extract" =>
    //      req.decode[Multipart[IO]] { m =>
    //        val partsMap = PartsMap(m.parts)
    //        for {
    /* maybeData <- DataSingle.getData(partsMap, relativeBase).attempt */
    //          schemaEngine       <- partsMap.optPartValue("schemaEngine")
    //          optSchemaFormatStr <- partsMap.optPartValue("schemaFormat")
    //          inference          <- partsMap.optPartValue("inference")
    //          label              <- partsMap.optPartValue("labelName")
    //          optBaseStr         <- partsMap.optPartValue("base")
    //          nodeSelector       <- partsMap.optPartValue("nodeSelector")
    //          schemaFormat <- optEither2f(
    //            optSchemaFormatStr,
    //            SchemaFormat.fromString
    //          )
    //          response <- maybeData match {
    //            // No data received
    //            case Left(err) =>
    //              errorResponseJson(err.getMessage, BadRequest)
    //            // Data received, try to extract
    //            case Right((resourceRdf, dp)) =>
    //              for {
    //                result <- io2f(
    //                  // Raise IO error if no node selector,
    //                  resourceRdf.use(rdf =>
    //                    dataExtract(
    //                      rdf,
    //                      dp.data,
    //                      dp.optDataFormat,
    //                      nodeSelector,
    //                      inference,
    //                      schemaEngine,
    //                      schemaFormat,
    //                      label,
    //                      None
    //                    )
    //                  )
    //                ).attempt
    /* .map(_.fold(exc => Left(exc.getMessage), res => Right(res))) */
    //                response <- result match {
    /* case Left(err) => errorResponseJson(err, InternalServerError) */
    //                  case Right(result) => Ok(result.toJson)
    //                }
    //
    //              } yield response
    //          }
    //        } yield response
    //      }

  }
  private val relativeBase = ApiDefaults.relativeBase

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
}
