package es.weso.rdfshape.server.api.routes.wikibase.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.operations.SchemaValidate
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get.WikibaseGetLabels
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.languages.WikibaseLanguages
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.query.WikibaseQueryOperation
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema.{
  WikibaseSchemaContent,
  WikibaseSchemaExtract,
  WikibaseSchemaValidate,
  WikibaseSheXerExtract
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.{
  WikibaseSearchEntity,
  WikibaseSearchLexeme,
  WikibaseSearchProperty
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.shapemaps.{Status => _}
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart

/** API service to handle wikibase (and mostly wikidata) related operations
  * Acts as an intermediate proxy between clients and the MediaWiki API
  *
  * @param client HTTP4S client object
  */
//noinspection DuplicatedCode
class WikibaseService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "wikibase"

  /** [[Client]] used for some queries, needs to follows some redirects to work properly
    */
  private val redirectClient: Client[IO] = FollowRedirect(3)(client)

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Search for wikidata objects and return their labels in the given languages.
      * Receives a JSON object with the input schema information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Entity identifier in the wikibase instance
      *  - languages [String]: Optionally, the languages of the results. Language
      *    codes separated by "|"
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      */
    case req @ GET -> Root / `api` / `verb` / "entityLabel" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val op = WikibaseGetLabels(opData, client)
            op.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Search for wikidata schemas using MediaWiki's API. Search based on lexeme labels.
      * Receives a JSON object with the input schema information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Schema identifier in the wikibase instance
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      */
    case req @ GET -> Root / `api` / `verb` / "schemaContent" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val op = WikibaseSchemaContent(opData, client)
            op.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Search for entities in a wikibase using MediaWiki's API. Search based on lexeme labels.
      * Receives a JSON object with the input property information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Keywords for the search
      *  - language [String]: Language in which the search is conducted
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      *
      * @note see https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      */
    case req @ GET -> Root / `api` / `verb` / "searchEntity" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val searchOperation = WikibaseSearchEntity(opData, client)
            searchOperation.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Search for properties in a wikibase using MediaWiki's API. Search based on property labels.
      * Receives a JSON object with the input property information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata
      *  - payload [String]: Keywords for the search
      *  - language [String]: Language in which the search is conducted
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      *
      * @note see https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      */
    case req @ GET -> Root / `api` / `verb` / "searchProperty" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val searchOperation = WikibaseSearchProperty(opData, client)
            searchOperation.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Search for lexemes in a wikibase using MediaWiki's API. Search based on lexeme labels.
      * Receives a JSON object with the input property information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Keywords for the search
      *  - language [String]: Language in which the search is conducted
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      *
      * @note see https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      */
    case req @ GET -> Root / `api` / `verb` / "searchLexeme" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val searchOperation = WikibaseSearchLexeme(opData, client)
            searchOperation.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Search for all the languages used in a wikibase instance.
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata.
      *    Returns a JSON object with the array of languages supported.
      *    See [[WikibaseOperationResult]]
      */
    case req @ GET -> Root / `api` / `verb` / "languages" =>
      for {
        // Get operation information sent by the user
        operationDetails <- WikibaseOperationDetails(req.params)
        // Create response
        response <- operationDetails.fold(
          err => errorResponseJson(err, BadRequest),
          opData => {
            val languagesOperation = WikibaseLanguages(opData, client)
            languagesOperation.performOperation
              .flatMap(results => Ok(results.asJson))
              .handleErrorWith(err =>
                errorResponseJson(err.getMessage, InternalServerError)
              )
          }
        )
      } yield response

    /** Execute a given SPARQL query to a given SPARQL endpoint of a wikibase instance.
      * Receives a target endpoint and the query text.
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata
      *  - payload [String]: SPARQL query to be run
      *    Returns a JSON object with the query results:
      *    (see [[WikibaseOperationResult]])
      *
      * Query examples in [[https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries/examples]]
      */
    case req @ POST -> Root / `api` / `verb` / "query" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get operation information sent by the user
          operationDetails <- WikibaseOperationDetails(partsMap)
          // Create response
          response <- operationDetails.fold(
            err => errorResponseJson(err, BadRequest),
            opData => {
              val queryOp = WikibaseQueryOperation(opData, client)
              queryOp.performOperation
                .flatMap(results => Ok(results.asJson))
                .handleErrorWith(err =>
                  errorResponseJson(err.getMessage, InternalServerError)
                )
            }
          )
        } yield response
      }

    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata.
      * Receives an entity URI as payload.
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata.
      *  - payload [String]: Unique URI of the entity in wikidata
      *    Returns a JSON object with the extracted schema:
      *    (see [[WikibaseOperationResult]])
      */
    case req @ POST -> Root / `api` / `verb` / "extract" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get operation information sent by the user
          operationDetails <- WikibaseOperationDetails(partsMap)
          // Create response
          response <- operationDetails.fold(
            err => errorResponseJson(err, BadRequest),
            opData => {
              val queryOp = WikibaseSchemaExtract(opData, redirectClient)
              queryOp.performOperation
                .flatMap(results => Ok(results.asJson))
                .handleErrorWith(err =>
                  errorResponseJson(err.getMessage, InternalServerError)
                )
            }
          )
        } yield response
      }

    // TODO: Needs exhaustive testing and client changes
    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata
      * using SheXer. See [[https://github.com/DaniFdezAlvarez/shexer]].
      * Receives an entity URI as payload.
      *  - endpoint [String]: Base URL of the target wikibase instance. Should
      *    be left empty so it defaults to Wikidata.
      *  - payload [String]: Unique URI of the entity in wikidata
      *    Returns a JSON object with the extracted schema:
      *    (see [[WikibaseOperationResult]])
      */
    case req @ POST -> Root / `api` / `verb` / "shexer" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get operation information sent by the user
          operationDetails <- WikibaseOperationDetails(partsMap)
          // Create response
          response <- operationDetails.fold(
            err => errorResponseJson(err, BadRequest),
            opData => {
              val queryOp = WikibaseSheXerExtract(opData, redirectClient)
              queryOp.performOperation
                .flatMap(results => Ok(results.asJson))
                .handleErrorWith(err =>
                  errorResponseJson(err.getMessage, InternalServerError)
                )
            }
          )
        } yield response
      }

    // TODO: Needs exhaustive testing and client changes
    /** Validate entities in wikidata using a given schema.
      * Receives an entity URI as payload, as well as the parameters to create
      * the ShEx schema against which to validate.
      *  - endpoint [String]: Base URL of the target wikibase instance. Should
      *    be left empty so it defaults to Wikidata.
      *  - payload [String]: Unique URI of the entity in wikidata
      *  - schema [String]: Schema data (raw, URL containing the schema or File with the schema)
      *  - schemaSource [String]: Identifies the source of the schema (raw, URL, file...)
      *  - schemaEngine [String]: Format of the schema, should be ShEx if using wikidata schemas
      *
      * Returns a JSON object with the validation results:
      * (see [[WikibaseOperationResult]] and
      * [[SchemaValidate.encodeSchemaValidateOperation]])
      */
    case req @ POST -> Root / `api` / `verb` / "validate" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          // Get operation information sent by the user
          eitherDetails <- WikibaseOperationDetails(partsMap)

          // Get the validation schema sent by the user
          eitherSchema <- Schema.mkSchema(partsMap)

          operationData: Either[String, (WikibaseOperationDetails, Schema)] =
            for {
              details <- eitherDetails
              schema  <- eitherSchema
            } yield (details, schema)

          // Create response
          response <- operationData.fold(
            err => errorResponseJson(err, BadRequest),
            {
              case (details, schema) => {
                val operation = WikibaseSchemaValidate(details, client, schema)
                operation.performOperation
                  .flatMap(results => Ok(results.asJson))
                  .handleErrorWith(err =>
                    errorResponseJson(err.getMessage, InternalServerError)
                  )
              }
            }
          )
        } yield response
      }
  }

}

object WikibaseService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Wikidata Service
    */
  def apply(client: Client[IO]): WikibaseService =
    new WikibaseService(client)
}
