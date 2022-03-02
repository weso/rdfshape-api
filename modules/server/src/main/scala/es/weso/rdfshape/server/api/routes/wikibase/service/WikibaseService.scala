package es.weso.rdfshape.server.api.routes.wikibase.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.operations.SchemaValidate
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationResult
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationResult._
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get.WikibaseGetLabels
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.languages.WikibaseLanguages
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.query.WikibaseQueryOperation
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema.{
  WikibaseSchemaContent,
  WikibaseSchemaExtract,
  WikibaseSchemaValidate,
  WikibaseSheXerExtract
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.WikibaseSearchTypes.WikibaseSearchTypes
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.{
  WikibaseSearchEntity,
  WikibaseSearchLexeme,
  WikibaseSearchProperty,
  WikibaseSearchTypes
}
import es.weso.rdfshape.server.api.routes.wikibase.service.operations.{
  WikibaseOperationInput,
  WikibaseValidateInput
}
import es.weso.shapemaps.{Status => _}
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes

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
  def routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    /** Search for wikidata objects and return their labels in the given languages.
      * Receives a JSON object with the input schema information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Entity identifier in the wikibase instance
      *  - languages [String]: Optionally, the languages of the results. Language
      *    codes separated by "|"
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      */
    POST / `verb` / "entityLabel" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseGetLabels(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    /** Search for wikidata schemas using MediaWiki's API. Search based on lexeme labels.
      * Receives a JSON object with the input schema information:
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata's.
      *  - payload [String]: Schema identifier in the wikibase instance
      *    Returns a JSON object with the results. See [[WikibaseOperationResult]]
      */
    POST / `verb` / "schemaContent" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseSchemaContent(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    /** Search for entities in a wikibase using MediaWiki's API. Search based on entity/property/lexeme labels.
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
    POST / `verb` / "search" / pathVar[WikibaseSearchTypes] ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { (searchType: String, body: WikibaseOperationInput) =>
      // Check for invalid search parameter
      if(!WikibaseSearchTypes.basicValues.contains(searchType))
        BadRequest(
          s"Invalid search type '$searchType'. Required one of: ${WikibaseSearchTypes.basicValues.mkString(", ")}"
        )
      else {
        // Create the corresponding operations and perform it
        val searchOperation = searchType match {
          case WikibaseSearchTypes.ENTITY =>
            WikibaseSearchEntity(body.operationDetails, client)
          case WikibaseSearchTypes.PROPERTY =>
            WikibaseSearchProperty(body.operationDetails, client)
          case WikibaseSearchTypes.LEXEME =>
            WikibaseSearchLexeme(body.operationDetails, client)
        }

        searchOperation.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    }

    /** Search for all the languages used in a wikibase instance.
      *  - endpoint [String]: Base URL of the target wikibase instance. Defaults to Wikidata.
      *    Returns a JSON object with the array of languages supported.
      *    See [[WikibaseOperationResult]]
      */
    POST / `verb` / "languages" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseLanguages(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    /** Execute a given SPARQL query to a given SPARQL endpoint of a wikibase instance.
      * Receives a target endpoint and the query text.
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata
      *  - payload [String]: SPARQL query to be run
      *    Returns a JSON object with the query results:
      *    (see [[WikibaseOperationResult]])
      *
      * Query examples in [[https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries/examples]]
      */
    POST / `verb` / "query" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseQueryOperation(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata.
      * Receives an entity URI as payload
      *    Returns a JSON object with the extracted schema:
      *    (see [[WikibaseOperationResult]])
      */
    POST / `verb` / "extract" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseSchemaExtract(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    // TODO: Needs exhaustive testing. Timeouts.
    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata
      * using SheXer. See [[https://github.com/DaniFdezAlvarez/shexer]].
      * Receives an entity URI as payload.
      *  - endpoint [String]: Base URL of the target wikibase instance. Should
      *    be left empty so it defaults to Wikidata.
      *  - payload [String]: Unique URI of the entity in wikidata
      *    Returns a JSON object with the extracted schema:
      *    (see [[WikibaseOperationResult]])
      */
    POST / `verb` / "shexer" ^ jsonOf[
      IO,
      WikibaseOperationInput
    ] |>> { body: WikibaseOperationInput =>
      val op = WikibaseSheXerExtract(body.operationDetails, client)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    // TODO: Needs exhaustive testing and client changes
    /** Validate entities in wikidata using a given schema.
      * Receives an entity URI as payload, as well as the parameters to create
      * the ShEx schema against which to validate.
      * Returns a JSON object with the validation results:
      * (see [[WikibaseOperationResult]] and
      * [[SchemaValidate.encodeSchemaValidateOperation]])
      */
    POST / `verb` / "validate" ^ jsonOf[
      IO,
      WikibaseValidateInput
    ] |>> { body: WikibaseValidateInput =>
      val op =
        WikibaseSchemaValidate(body.operationDetails, client, body.schema)
      op.performOperation
        .flatMap(results => Ok(results.asJson))
        .handleErrorWith(err => InternalServerError(err.getMessage))

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
