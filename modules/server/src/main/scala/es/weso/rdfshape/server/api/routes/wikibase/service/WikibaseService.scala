package es.weso.rdfshape.server.api.routes.wikibase.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.ApiService
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
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.TypeParameter
import es.weso.shapemaps.{Status => _}
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._

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

    "Search for wikidata objects and return their labels in the given languages" **
      POST / `verb` / "entityLabel" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseGetLabels(body.operationDetails, client)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Search for wikidata schemas using MediaWiki's API. Search based on lexeme labels" **
      POST / `verb` / "schemaContent" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseSchemaContent(body.operationDetails, client)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    /** @note see https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      */
    s"""Search for entities in a wikibase using MediaWiki's API.
       | Search based on ${WikibaseSearchTypes.basicValues.mkString(
      "/"
    )} labels""".stripMargin **
      POST / `verb` / "search" / pathVar[WikibaseSearchTypes](
        WikibaseServiceDescriptions.WikibaseSearchType.name,
        WikibaseServiceDescriptions.WikibaseSearchType.description
      ) ^ jsonOf[
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

    "Search for all the languages used in a wikibase instance" **
      POST / `verb` / "languages" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseLanguages(body.operationDetails, client)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Execute a given SPARQL query against a given SPARQL endpoint of a wikibase instance" **
      POST / `verb` / "query" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseQueryOperation(body.operationDetails, client)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Attempt to extract an schema (ShEx) from a given entity present in wikidata" **
      POST / `verb` / "extract" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseSchemaExtract(body.operationDetails, redirectClient)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    // TODO: Timeouts. Point to the correct SheXer instance when known.
    """Attempt to extract an schema (ShEx) from a given entity present
      |in wikidata using SheXer (https://github.com/DaniFdezAlvarez/shexer)""".stripMargin **
      POST / `verb` / "shexer" ^ jsonOf[
        IO,
        WikibaseOperationInput
      ] |>> { body: WikibaseOperationInput =>
        val op = WikibaseSheXerExtract(body.operationDetails, redirectClient)
        op.performOperation
          .flatMap(results => Ok(results.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Validate entities in wikidata using a given schema" **
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

/** Compendium of additional text constants used to describe inline parameters
  * (query and path parameters) in Swagger
  */
private object WikibaseServiceDescriptions {
  case object WikibaseSearchType {
    val name: String = TypeParameter.name
    val description =
      s"Type of search being made in Wikibase. One of: ${WikibaseSearchTypes.basicValues.mkString(", ")}"
  }
}
