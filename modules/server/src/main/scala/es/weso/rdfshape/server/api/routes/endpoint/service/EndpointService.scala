package es.weso.rdfshape.server.api.routes.endpoint.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.endpoint.logic.Endpoint
import es.weso.rdfshape.server.api.routes.endpoint.logic.Endpoint.{getEndpointAsRDFReader, getEndpointInfo}
import es.weso.rdfshape.server.api.routes.endpoint.logic.Outgoing._
import es.weso.rdfshape.server.api.routes.endpoint.service.operations.EndpointOutgoingInput
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.implicits.query_parsers.urlQueryParser
import es.weso.utils.IOUtils.io2es
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.rho.RhoRoutes

import java.net.URL

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
  def routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    /** Check the existence of an endpoint and get its response, if any
     * Receives a JSON object with the input endpoint:
     *  - endpoint [URL]: Target endpoint
     *  - query [String]: User query with content and source
     * Returns a JSON object with the results (see [[Endpoint.encoder]]).
     */
    POST / `api` / `verb` / "info" ^ jsonOf[
      IO,
      EndpointOutgoingInput
    ] |>> { (body: EndpointOutgoingInput) =>
      body match {
        case EndpointOutgoingInput(endpointUrl, queryObject) =>

          val ioResponse = for {
            endpoint <- getEndpointAsRDFReader(endpointUrl)
            _ = logger.debug(s"Query to \"$endpointUrl\": \"${queryObject.raw}\"")
            queryResponse  <- io2es(endpoint.queryAsJson(queryObject.raw))
          } yield queryResponse

          ioResponse.value.flatMap {
            case Left(err) => InternalServerError(err)
            case Right(json) => Ok(json)
          }
      }
    }

    /** Perform a SPARQL query targeted to a specific endpoint.
      * Receives a JSON object with the input endpoint query:
      *  - endpoint [URL]: Query target endpoint
      *  - query [String]: User query with content and source
      *    Returns a JSON object with the query results:
      *    - head [Object]: Query metadata
      *      - vars: [Array]: Query variables
      *    - results [Object]: Query results
      *      - bindings: [Array]: Query results, each item being an object mapping each variable to its value
      */
    /**
      */
    GET / `api` / `verb` / "info" +?
      param[URL](EndpointParameter.name) |>> { (endpointUrl: URL) =>
      Ok(getEndpointInfo(endpointUrl).asJson)
    }

    /** Attempt to contact a wikibase endpoint and return the data (triplets) about a node in it.
      * Receives a JSON object with the input endpoint, node and limits:
      *  - endpoint [URL]: Query target endpoint
      *  - node [String]: Node identifier in the target wikibase
      *  - limit [Int]: Max number of results, defaults to one
      *    Returns a JSON object with the endpoint response:
      *    - endpoint [String]: Target endpoint
      *    - node [String]: Node identifier in the target wikibase
      *    - children [Array]: List of returned objects, each being a triplet:
      *      - pred: [String]: Predicate identifier in the target wikibase
      *      - values: [Array]: List of raw values for the entity and predicate
      */
    GET / `api` / `verb` / "outgoing" +?
      param[URL](EndpointParameter.name) &
      param[String](NodeParameter.name) &
      param[Option[Int]](LimitParameter.name) |>> {
        (endpointUrl: URL, node: String, limit: Option[Int]) =>
          for {
            eitherOutgoing <- getOutgoing(
              endpointUrl,
              node,
              limit
            ).value
            resp <- eitherOutgoing.fold(
              err => InternalServerError(s"Error: $err"),
              outgoing => Ok(outgoing.asJson)
            )
          } yield resp
      }

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
