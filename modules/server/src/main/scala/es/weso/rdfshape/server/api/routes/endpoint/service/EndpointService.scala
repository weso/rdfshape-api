package es.weso.rdfshape.server.api.routes.endpoint.service

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.{Endpoint => EndpointJena}
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.IncomingRequestParameters.{
  LimitParam,
  OptEndpointParam,
  OptNodeParam
}
import es.weso.rdfshape.server.api.routes.PartsMap
import es.weso.rdfshape.server.api.routes.endpoint.logic.Endpoint.{
  getEndpointAsRDFReader,
  getEndpointInfo,
  getEndpointUrl
}
import es.weso.rdfshape.server.api.routes.endpoint.logic.{
  Endpoint,
  Outgoing,
  SparqlQuery,
  SparqlQueryParam
}
import es.weso.rdfshape.server.utils.numeric.NumericUtils
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
    with LazyLogging {

  private val relativeBase = ApiDefaults.relativeBase

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / `api` / "endpoint" / "query" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        val r: EitherT[IO, String, Json] = for {
          endpointUrl <- getEndpointUrl(partsMap)
          endpoint    <- getEndpointAsRDFReader(endpointUrl)
          either <- EitherT
            .liftF[IO, String, Either[
              String,
              (SparqlQuery, SparqlQueryParam)
            ]](
              SparqlQueryParam.getSparqlQuery(partsMap)
            )
          pair <- EitherT.fromEither[IO](either)
          (_, qp)     = pair
          optQueryStr = qp.queryRaw
          json <- {
            logger.debug(
              s"Query to endpoint $endpoint: ${optQueryStr.getOrElse("")}"
            )
            io2es(endpoint.queryAsJson(optQueryStr.getOrElse("")))
          }
        } yield json

        for {
          either <- r.value
          resp <- either.fold(
            e => errJson(s"Error querying endpoint: $e"),
            json => Ok(json)
          )
        } yield resp
      }

    case req @ POST -> Root / `api` / "endpoint" / "info" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          val r: EitherT[IO, String, Json] = for {
            endpointUrl <- getEndpointUrl(partsMap)
            ei <- EitherT.liftF[IO, String, Endpoint](
              getEndpointInfo(endpointUrl, client)
            )
          } yield ei.asJson
          for {
            either <- r.value
            resp <- either.fold(
              e => errJson(s"Error obtaining info on Endpoint $e"),
              json => Ok(json)
            )
          } yield resp
        }
      }

    case GET -> Root / `api` / "endpoint" / "outgoing" :?
        OptEndpointParam(optEndpoint) +&
        OptNodeParam(optNode) +&
        LimitParam(optLimit) =>
      for {
        eitherOutgoing <- getOutgoing(optEndpoint, optNode, optLimit).value
        resp <- eitherOutgoing.fold(
          (s: String) => errJson(s"Error: $s"),
          (outgoing: Outgoing) => Ok(outgoing.toJson)
        )
      } yield resp

  }

  private def errJson(msg: String): IO[Response[IO]] =
    Ok(Json.fromFields(List(("error", Json.fromString(msg)))))

  private def getOutgoing(
      optEndpoint: Option[String],
      optNode: Option[String],
      optLimit: Option[String]
  ): EitherT[IO, String, Outgoing] = {
    for {
      endpointIRI <- EitherT.fromEither[IO](
        Either
          .fromOption(optEndpoint, "No endpoint provided")
          .flatMap(IRI.fromString(_))
      )
      node <- EitherT.fromEither[IO](
        Either
          .fromOption(optNode, "No node provided")
          .flatMap(IRI.fromString(_))
      )
      limit <- EitherT.fromEither[IO](
        NumericUtils.parseInt(optLimit.getOrElse("1"))
      )
      o <- outgoing(endpointIRI, node, limit)
    } yield o
  }

  private def outgoing(endpoint: IRI, node: IRI, limit: Int): ESIO[Outgoing] =
    for {
      triples <- stream2es(EndpointJena(endpoint).triplesWithSubject(node))
    } yield Outgoing.fromTriples(node, endpoint, triples.toSet)

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
