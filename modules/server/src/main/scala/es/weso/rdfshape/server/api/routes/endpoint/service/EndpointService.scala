package es.weso.rdfshape.server.api.routes.endpoint.service

import cats.data.EitherT
import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.endpoint.logic.Endpoint.{
  getEndpointAsRDFReader,
  getEndpointInfo,
  getEndpointUrl
}
import es.weso.rdfshape.server.api.routes.endpoint.logic.EndpointStatus._
import es.weso.rdfshape.server.api.routes.endpoint.logic.Outgoing.getOutgoing
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery.mkSparqlQuery
import es.weso.rdfshape.server.api.routes.endpoint.logic.{Endpoint, Outgoing}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  EndpointParameter,
  LimitParameter,
  NodeParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.utils.IOUtils._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.multipart._

/** API service to handle endpoints and operations targeted to them (queries, etc.)
  *
  * @param client HTTP4S client object
  */
class EndpointService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "endpoint"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Perform a SPARQL query targeted to a specific endpoint.
      * Receives a JSON object with the input endpoint query:
      *  - query [String]: User input for the query
      *  - querySource [String]: Identifies the source of the query (raw, URL, file...)
      *    Returns a JSON object with the query results:
      *    - head [Object]: Query metadata
      *      - vars: [Array]: Query variables
      *    - results [Object]: Query results
      *      - bindings: [Array]: Query results, each item being an object mapping each variable to its value
      */
    case req @ POST -> Root / `api` / `verb` / "query" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        val r: EitherT[IO, String, Json] = for {
          endpointUrl <- getEndpointUrl(partsMap)
          endpoint    <- getEndpointAsRDFReader(endpointUrl)
          either <- EitherT
            .liftF[IO, String, Either[
              String,
              SparqlQuery
            ]](
              mkSparqlQuery(partsMap)
            )
          eitherQuery <- EitherT.fromEither[IO](either)

          json <- {

            eitherQuery.rawQuery.fold(
              err => EitherT.left(IO.pure(err)),
              raw => {
                logger.debug(s"Query to endpoint $endpoint: $raw")
                io2es(endpoint.queryAsJson(raw))
              }
            )
          }
        } yield json

        for {
          either <- r.value
          resp <- either.fold(
            e =>
              errorResponseJson(
                s"Query failed. $e",
                InternalServerError
              ),
            json => Ok(json)
          )
        } yield resp
      }

    /** Attempt to contact an endpoint and return metadata about it.
      * Receives a JSON object with the input endpoint:
      *  - endpoint [String]: Target endpoint
      *    Returns a JSON object with the endpoint response:
      *    - head [Object]: Query metadata
      *      - vars: [Array]: Query variables
      *    - results [Object]: Query results
      *      - bindings: [Array]: Query results, each item being an object mapping each variable to its value
      */
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          endpointUrl <- getEndpointUrl(partsMap).value
          response <- endpointUrl match {
            case Left(err) => errorResponseJson(err, BadRequest)
            case Right(endpointUrl) =>
              val endpointInfo = getEndpointInfo(endpointUrl)
              endpointInfo match {
                case Endpoint(errMsg, OFFLINE) =>
                  errorResponseJson(
                    errMsg,
                    InternalServerError
                  )
                case _ => Ok(endpointInfo.asJson)
              }
          }
        } yield response

      }

    /** Attempt to contact a wikibase endpoint and return the data (triplets) about a node in it.
      * Receives a JSON object with the input endpoint, node and limits:
      *  - endpoint [String]: Target endpoint
      *  - node [String]: Node identifier in the target wikibase
      *  - limit [Int]: Max number of results
      *    Returns a JSON object with the endpoint response:
      *    - endpoint [String]: Target endpoint
      *    - node [String]: Node identifier in the target wikibase
      *    - children [Array]: List of returned objects, each being a triplet:
      *      - pred: [String]: Predicate identifier in the target wikibase
      *      - values: [Array]: List of raw values for the entity and predicate
      */
    case GET -> Root / `api` / `verb` / "outgoing" :?
        EndpointParameter(optEndpoint) +&
        NodeParameter(optNode) +&
        LimitParameter(optLimit) =>
      for {
        eitherOutgoing <- getOutgoing(optEndpoint, optNode, optLimit).value
        resp <- eitherOutgoing.fold(
          (s: String) => errorResponseJson(s"Error: $s", InternalServerError),
          (outgoing: Outgoing) => Ok(outgoing.toJson)
        )
      } yield resp

  }

}

object EndpointService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Endpoint Service
    */
  def apply(client: Client[IO]): EndpointService =
    new EndpointService(client)
}
