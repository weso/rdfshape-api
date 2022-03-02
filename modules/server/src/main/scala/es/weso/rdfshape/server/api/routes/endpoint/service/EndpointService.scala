package es.weso.rdfshape.server.api.routes.endpoint.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.endpoint.logic.Endpoint.getEndpointInfo
import es.weso.rdfshape.server.api.routes.endpoint.logic.Outgoing._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.implicits.query_parsers.urlQueryParser
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._

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

    "Check the existence of an endpoint and get its response, if any" **
      GET / `verb` / "info" +?
      param[URL](EndpointParameter.name) |>> { (endpointUrl: URL) =>
        Ok(getEndpointInfo(endpointUrl).asJson)
      }

    "Attempt to contact a wikibase endpoint and return the data (triplets) about a node in it" **
      GET / `verb` / "outgoing" +?
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
