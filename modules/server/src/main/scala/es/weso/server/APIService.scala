package es.weso.server

import cats.effect._
import cats.implicits._
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.ApiHelper._
import results._
import es.weso.server.Defaults.{
  availableDataFormats,
  availableInferenceEngines,
  defaultActiveDataTab,
  defaultDataFormat,
  defaultInference
}
import es.weso.server.QueryParams._
import es.weso.server.helper.DataFormat
import es.weso.server.utils.Http4sUtils._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart
import org.http4s.server.staticcontent.{ResourceService, resourceServiceBuilder}
import org.log4s.getLogger

import scala.concurrent.duration._
import APIDefinitions._
import cats.Monad
import cats.data.EitherT
import es.weso.html
import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.IRI
import org.http4s.dsl.io.Ok
import es.weso.utils.IOUtils._
import scala.util.Try
import org.http4s.Uri.{Path => UriPath}

class APIService(client: Client[IO]) extends Http4sDsl[IO] {

  private val relativeBase = Defaults.relativeBase
  private val logger       = getLogger

  private val swagger =
    resourceServiceBuilder[IO]("/swagger") // ResourceService.Config())

  val routes = HttpRoutes.of[IO] {

    case req @ GET -> Root / `api` / "health" =>
      for {
        _    <- IO { pprint.log(req) }
        resp <- Ok("OK")
      } yield resp

    case req @ GET -> Root / `api` / "endpoint" / "outgoing" :?
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

    // Contents on /swagger are directly mapped to /swagger
    /* case r @ GET -> _ if
     * r.pathInfo.startsWith(UriPath.fromString("/swagger/")) =>
     * swagger.toRoutes. // getOrElseF(NotFound()) */

  }

  private def parseInt(s: String): Either[String, Int] =
    Try(s.toInt).map(Right(_)).getOrElse(Left(s"$s is not a number"))

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
      limit <- EitherT.fromEither[IO](parseInt(optLimit.getOrElse("1")))
      o     <- outgoing(endpointIRI, node, limit)
    } yield o
  }

  private def outgoing(endpoint: IRI, node: IRI, limit: Int): ESIO[Outgoing] =
    for {
      triples <- stream2es(Endpoint(endpoint).triplesWithSubject(node))
    } yield Outgoing.fromTriples(node, endpoint, triples.toSet)

  //    Monad[F].pure(Left(s"Not implemented"))

}

object APIService {
  def apply(client: Client[IO]): APIService =
    new APIService(client)
}
