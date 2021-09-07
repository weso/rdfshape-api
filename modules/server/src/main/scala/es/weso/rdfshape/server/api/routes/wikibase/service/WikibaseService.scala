package es.weso.rdfshape.server.api.routes.wikibase.service

import cats.data._
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaOperations.schemaResult2json
import es.weso.rdfshape.server.api.routes.wikibase.logic.WikibaseEntity.{
  uriToEntity,
  uriToEntity2
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.{
  WikibaseEntity,
  WikibaseSchemaParam
}
import es.weso.rdfshape.server.api.routes.wikibase.service.WikibaseServiceUtils.{
  convertEntities,
  convertLanguages,
  mkShexerParams
}
import es.weso.rdfshape.server.api.utils.OptEitherF.ioFromEither
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.schema.{Schema, ShapeMapTrigger}
import es.weso.schemaInfer.{InferOptions, SchemaInfer}
import es.weso.shapemaps.{Status => _, _}
import es.weso.utils.IOUtils._
import es.weso.wikibaserdf._
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.middleware.FollowRedirect
import org.http4s.dsl._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.multipart._

/** API service to handle wikibase (and mostly wikidata) related operations
  * Acts as an intermediate proxy between clients and the MediaWiki API
  *
  * @param client HTTP4S client object
  */
class WikibaseService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "wikidata"

  val wikidataUrl                = "https://www.wikidata.org"
  val wikidataUri                = uri"https://www.wikidata.org"
  val wikidataEntityUrl          = uri"https://www.wikidata.org/entity"
  val apiUri                     = uri"/api/wikidata/entity"
  val wikidataQueryUri: Uri      = uri"https://query.wikidata.org/sparql"
  val defaultLimit               = 20
  val defaultContinue            = 0
  val redirectClient: Client[IO] = FollowRedirect(3)(client)

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Search for wikidata entities using MediaWiki's API. Search based on entity ID
      * See https://www.wikidata.org/w/api.php?action=help&modules=wbgetentities
      * Receives a Wikidata entity label and a language and fetches entities in Wikidata
      *  - wdEntity [String]: Wikidata entity label
      *  - language [String]: Response desired language
      *    Returns a JSON object after querying MediaWiki's "wbgetentities" endpoint
      */
    case GET -> Root / `api` / `verb` / "entityLabel" :?
        WdEntityParameter(entity) +&
        LanguageParameter(language) =>
      val uri = wikidataUri
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbgetentities")
        .withQueryParam("props", "labels")
        .withQueryParam("ids", entity)
        .withQueryParam("languages", language)
        .withQueryParam("format", "json")

      logger.debug(s"wikidata searchEntity uri: ${uri.toString}")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        either <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        resp <- Ok(either.fold(Json.fromString, identity))
      } yield resp

    /** Search for wikidata schemas using MediaWiki's API.
      * Receives a Wikidata schema label and fetches schemas in Wikidata
      *  - wdSchema [String]: Wikidata schema label
      *    Returns a JSON object after manually querying the schema's page
      */
    case GET -> Root / `api` / `verb` / "schemaContent" :?
        WdSchemaParameter(wdSchema) =>
      val uri = wikidataUri.withPath(
        Uri.Path.unsafeFromString(s"/wiki/Special:EntitySchemaText/$wdSchema")
      )

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[String].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[String]
              )
        }
        json: Json = eitherValues.fold(
          e => Json.fromFields(List(("error", Json.fromString(e)))),
          s => Json.fromFields(List(("result", Json.fromString(s))))
        )
        resp <- Ok(json)
      } yield resp

    /** Search for entities in a wikibase using MediaWiki's API. Search based on entity labels.
      * See https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      * Receives an entity label and a language and fetches entities in the wikibase whose endpoint was selected
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata
      *  - label [String]: Label / keywords in the name of the entities searched
      *  - language [String]: Response desired language
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object after querying MediaWiki's "wbsearchentities" endpoint
      */
    case GET -> Root / `api` / `verb` / "searchEntity" :?
        EndpointParameter(maybeEndpoint) +&
        LabelParameter(label) +&
        LanguageParameter(language) +&
        LimitParameter(maybelimit) +&
        ContinueParameter(maybeContinue) =>
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)
      val endpoint: String = maybeEndpoint.getOrElse(wikidataUrl)

      logger.debug(s"Wikibase entity search with endpoint: $endpoint")

      val uri = Uri
        .unsafeFromString(endpoint)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("format", "json")

      logger.debug(s"wikidata searchEntity uri: $uri")

      val req: Request[IO] = Request(method = GET, uri = uri)

      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- convertEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp

    /** Search for properties in a wikibase using MediaWiki's API. Search based on property labels.
      * See https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      * Receives a property label and a language and fetches properties in the wikibase whose endpoint was selected
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata
      *  - label [String]: Label / keywords in the name of the properties searched
      *  - language [String]: Response desired language
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object after querying MediaWiki's "wbsearchentities" endpoint.
      */
    case GET -> Root / `api` / `verb` / "searchProperty" :?
        EndpointParameter(maybeEndpoint) +&
        LabelParameter(label) +&
        LanguageParameter(language) +&
        LimitParameter(maybelimit) +&
        ContinueParameter(maybeContinue) =>
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)
      val endpoint: String = maybeEndpoint.getOrElse(wikidataUrl)

      logger.debug(s"Wikibase property search with endpoint: $endpoint")

      val uri = Uri
        .fromString(endpoint)
        .valueOr(throw _)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("type", "property")
        .withQueryParam("format", "json")

      logger.debug(s"wikidata searchProperty uri: $uri")

      val req: Request[IO] = Request(method = GET, uri = uri)

      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- convertEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp

    /** Search for lexemes in a wikibase using MediaWiki's API. Search based on lexeme labels.
      * See https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
      * Receives a lexeme label and a language and fetches properties in the wikibase whose endpoint was selected
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata
      *  - label [String]: Label / keywords in the name of the lexemes searched
      *  - language [String]: Response desired language
      *  - limit [Int]: Max number of results
      *  - continue [Int]: Offset where to continue a search
      *    Returns a JSON object after querying MediaWiki's "wbsearchentities" endpoint.
      */
    case GET -> Root / `api` / `verb` / "searchLexeme" :?
        EndpointParameter(maybeEndpoint) +&
        LabelParameter(label) +&
        LanguageParameter(language) +&
        LimitParameter(maybelimit) +&
        ContinueParameter(maybeContinue) =>
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)
      val endpoint: String = maybeEndpoint.getOrElse(wikidataUrl)

      logger.debug(s"Wikibase lexeme search with endpoint: $endpoint")

      val uri = Uri
        .fromString(endpoint)
        .valueOr(throw _)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("type", "lexeme")
        .withQueryParam("format", "json")

      logger.debug(s"wikidata searchLexeme uri: $uri")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- convertEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp

    /** Search for all the languages used in a wikibase instance.
      *  - endpoint [String]: SPARQL query endpoint of the target wikibase instance. Defaults to Wikidata.
      *    Returns a JSON object with the array of languages returned by the endpoint.
      */
    case GET -> Root / `api` / `verb` / "languages" :?
        EndpointParameter(maybeEndpoint) =>
      val endpoint: String = maybeEndpoint.getOrElse(wikidataUrl)
      logger.debug(s"Wikibase language search with endpoint: $endpoint")

      val uri = Uri
        .fromString(endpoint)
        .valueOr(throw _)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "query")
        .withQueryParam("meta", "wbcontentlanguages")
        .withQueryParam("wbclcontext", "term")
        .withQueryParam("wbclprop", "code|autonym")
        .withQueryParam("format", "json")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- convertLanguages(json)
        } yield converted
        resp <- Ok(
          eitherResult.fold(Json.fromString, identity)
        )
      } yield resp

    /** Execute a given SPARQL query to a given SPARQL endpoint of a wikibase instance.
      * Receives a target endpoint and the query text.
      *  - endpoint [String]: SPARQL query endpoint. Defaults to Wikidata
      *  - query [String]: SPARQL query to be run
      *    Returns a JSON object with the query results:
      *    - head [Object]: Query metadata
      *      - vars: [Array]: Query variables
      *    - results [Object]: Query results
      *      - bindings: [Array]: Query results, each item being an object mapping each variable to its value
      */
    case req @ POST -> Root / `api` / `verb` / "query" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          for {
            optQuery    <- partsMap.optPartValue("query")
            optEndpoint <- partsMap.optPartValue("endpoint")
            endpoint = optEndpoint.getOrElse(wikidataQueryUri.toString())
            query    = optQuery.getOrElse("")
            req: Request[IO] =
              Request(
                method = GET,
                uri = Uri
                  .fromString(endpoint)
                  .valueOr(throw _)
                  .withQueryParam("query", query)
              )
                .withHeaders(
                  `Accept`(MediaType.application.`json`)
                )
            eitherValue <- client.run(req).use {
              case Status.Successful(r) =>
                r.attemptAs[Json].leftMap(_.message).value
              case r =>
                r.as[String]
                  .map(b =>
                    s"Request $req failed with status ${r.status.code} and body $b"
                      .asLeft[Json]
                  )
            }
            resp <- Ok(eitherValue.fold(Json.fromString, identity))
          } yield resp
        }
      }

    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata.
      * Receives an entity URI:
      *  - entity [String]: Unique address of the entity in wikidata
      *    Returns a JSON object with the extraction results:
      *  - entity [String]: URI of the entity whose information we searched
      *  - result [String]: Extracted schema
      */
    case req @ POST -> Root / `api` / `verb` / "extract" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: EitherT[IO, String, Response[IO]] = for {
          label <- EitherT(partsMap.eitherPartValue("entity"))
          info  <- either2es[WikibaseEntity](uriToEntity(label))
          _ <- {
            logger.debug(s"Extraction URI: ${info.uri}");
            ok_esf[Unit, IO](())
          }
          strRdf <- io2es(redirectClient.expect[String](info.uri))
          eitherInferred <- io2es(
            RDFAsJenaModel
              .fromString(strRdf, "TURTLE")
              .flatMap(
                _.use(rdf =>
                  for {
                    rdfSerialized <- rdf.serialize("TURTLE")
                    nodeSelector = RDFNodeSelector(IRI(label))
                    inferred <- SchemaInfer.runInferSchema(
                      rdf,
                      nodeSelector,
                      "ShEx",
                      IRI(s"http://example.org/Shape_${info.localName}"),
                      InferOptions.defaultOptions.copy(maxFollowOn = 3)
                    )
                  } yield inferred
                )
              )
          )
          pair <- either2es[(Schema, ResultShapeMap)](eitherInferred)
          shExCStr <- io2es({
            val (schema, _) = pair
            schema.serialize("SHEXC")
          })
          _ <- {
            logger.trace(s"ShExC str: $shExCStr");
            ok_es[Unit](())
          }
          resp <- io2es(
            Ok(
              Json.fromFields(
                List(
                  ("entity", Json.fromString(label)),
                  ("result", Json.fromString(shExCStr))
                )
              )
            )
          )
        } yield resp
        for {
          either <- r.value
          resp <- either.fold(
            err => errorResponseJson(err, InternalServerError),
            r => IO.pure(r)
          )
        } yield resp
      }

    // TODO: This one doesn't work. It gives a timeout response
    /** Attempts to extract an schema (ShEx) from a given entity present in wikidata using "shexer".
      * See https://github.com/DaniFdezAlvarez/shexer
      * Receives an entity URI:
      *  - entity [String]: Unique address of the entity in wikidata
      *    Returns a JSON object with the extraction results:
      *  - entity [String]: URI of the entity whose information we searched
      *  - result [String]: Extracted schema
      */
    case req @ POST -> Root / `api` / `verb` / "shexer" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: EitherT[IO, String, Response[IO]] = for {
          label      <- EitherT(partsMap.eitherPartValue("entity"))
          jsonParams <- either2es[Json](mkShexerParams(label))
          postRequest = Request[IO](
            method = POST,
            uri = uri"http://156.35.94.158:8081/shexer"
          ).withHeaders(`Content-Type`(MediaType.application.`json`))
            .withEntity[Json](jsonParams)
          _ <- {
            logger.debug(s"URI: ${jsonParams.spaces2}");
            ok_es[Unit](())
          }
          result <- f2es(redirectClient.expect[Json](postRequest))
          _ <- {
            logger.trace(s"Result\n${result.spaces2}");
            ok_es[Unit](())
          }
          resp <- f2es(Ok(result))
        } yield resp
        for {
          either <- r.value
          resp <- either.fold(
            err => errorResponseJson(err, InternalServerError),
            r => IO.pure(r)
          )
        } yield resp
      }

    /** Validate entities in a wikibase using wikidata schemas or shape expressions.
      * Receives several data:
      *  - endpoint [String]: Endpoint of the target wikibase instance. Defaults to Wikidata
      *  - entity [String]: URI of the entity to be validated
      *  - entitySchema [String]: (Wikidata schema only) Identifier of the wikidata schema to be used
      *  - schema [String]: (ShEx schema only) Raw contents of the schema supplied by the user
      *  - schemaFormat [String]: (ShEx schema only) Format of the schema supplied by the user
      *  - schemaEngine [String]: Schema engine to be used (defaults to ShEx)
      *  - shape [String]: Shape of the schema which will be compared against the entity
      *    Returns a JSON object with the results (pending).
      */
    case req @ POST -> Root / `api` / `verb` / "validate" =>
      logger.debug(s"Wikidata validate request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: IO[Response[IO]] = for {
          eitherItem <- partsMap.eitherPartValue("entity")
          _ <- {
            logger.debug(eitherItem.toString);
            IO.pure(())
          }
          item <- ioFromEither(eitherItem)
          _ <- {
            logger.debug(item);
            IO.pure(())
          }
          info <- ioFromEither(uriToEntity2(item))
          _ <- {
            logger.debug(info.toString);
            IO.pure(())
          }
          pair <- WikibaseSchemaParam.mkSchema(partsMap, None, client)
          _ <- {
            logger.debug(pair.toString());
            IO.pure(())
          }
          (schema, wbp) = pair
          iriItem  <- ioFromEither(IRI.fromString(info.sourceUri))
          shapeMap <- ioFromEither(ShapeMap.empty.add(iriItem, Start))
          triggerMode = ShapeMapTrigger(shapeMap)
          result <- for {
            res1 <- WikibaseRDF.wikidata
            res2 <- RDFAsJenaModel.empty
            vv <- (res1, res2).tupled.use { case (rdf, builder) =>
              for {
                r    <- schema.validate(rdf, triggerMode, builder)
                json <- schemaResult2json(r)
              } yield json
            }
          } yield vv
          resp <- Ok(result)
        } yield resp
        r.attempt.flatMap(
          _.fold(
            s => errorResponseJson(s.getMessage, InternalServerError),
            IO.pure
          )
        )
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
