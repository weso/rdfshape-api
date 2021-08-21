package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.{Endpoint => EndpointJena}
import es.weso.rdfshape.server.api.routes.PartsMap
import es.weso.utils.IOUtils.{ESIO, io2es}
import io.circe.Json
import org.http4s.client.Client

import java.net.URL
import scala.util.{Failure, Success, Try}

/** Data class representing an endpoint
  *
  * @param msg    Message attached to the information/returned by the endpoint
  * @param status Status of the endpoint
  */
case class Endpoint(msg: String, status: Option[String] = None) {
  def asJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg)),
      ("status", Json.fromString(status.getOrElse("")))
    )
  )
}

/** Static utilities used by the {@link es.weso.rdfshape.server.api.routes.endpoint.service.EndpointService}
  * to operate on endpoints
  */
private[api] object Endpoint extends LazyLogging {

  /** Fetch information from an endpoint and return the RDF Reader to operate the information
    *
    * @param url Endpoint URL
    * @return An RDF Reader to operate the information in the endpoint
    */
  def getEndpointAsRDFReader(url: URL): ESIO[RDFReader] =
    io2es(EndpointJena.fromString(url.toString))

  /** Given an endpoint URL, fetch and return its data
    *
    * @param url    Endpoint URL
    * @param client Client used to fetch the URL
    * @return An instance of EndpointInfo with the information contained in the endpoint
    */
  def getEndpointInfo(url: URL, client: Client[IO]): IO[Endpoint] = {
    IO.println(s"Obtaining info of endpoint $url") *>
      client.expect[String](url.toString).map(Endpoint(_))
  }

  /** Given a request's parameters, try to extract an endpoint URL from them
    *
    * @param partsMap Request's parameter
    * @return The endpoint URL or an error message
    */
  def getEndpointUrl(
      partsMap: PartsMap
  ): EitherT[IO, String, URL] = for {
    maybeStr <- EitherT.liftF[IO, String, Option[String]](
      partsMap.optPartValue("endpoint")
    )
    ep <- maybeStr match {
      case None =>
        EitherT.leftT[IO, URL](s"No value for param endpoint")
      case Some(str) =>
        Try(new URL(str)) match {
          case Success(url) => EitherT.rightT[IO, String](url)
          case Failure(ex)  => EitherT.leftT[IO, URL](ex.getMessage)
        }
    }
  } yield ep
}
