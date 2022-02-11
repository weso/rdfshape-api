package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.{Endpoint => EndpointJena}
import es.weso.rdfshape.server.api.routes.endpoint.logic.EndpointStatus.{
  EndpointStatus,
  OFFLINE,
  ONLINE
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.utils.IOUtils.{ESIO, io2es}
import io.circe.Json

import java.net.URL
import scala.util.{Failure, Success, Try}

/** Data class representing an endpoint
  *
  * @param msg    Message attached to the information/returned by the endpoint
  * @param status Status of the endpoint
  */
sealed case class Endpoint(msg: String, status: EndpointStatus) {
  def asJson: Json = Json.fromFields(
    List(
      ("message", Json.fromString(msg)),
      ("status", Json.fromString(status))
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
    * @return An instance of Endpoint with the information contained in the endpoint
    */
  def getEndpointInfo(url: URL): Endpoint = {
    logger.debug(s"Obtaining info of endpoint $url")
    getUrlContents(url.toString) match {
      case Left(errMsg)    => Endpoint(errMsg, OFFLINE)
      case Right(response) => Endpoint(response, ONLINE)
    }
  }

  /** Given a request's parameters, try to extract an endpoint URL from them
    *
    * @param partsMap Request's parameter
    * @return The endpoint URL or an error message
    */
  def getEndpointUrl(
      partsMap: PartsMap
  ): IO[Either[String, URL]] = for {
    maybeStr <- partsMap.optPartValue("endpoint").map(_.toRight(""))

    ep = maybeStr match {
      case Left(_) =>
        val msg = s"No value provided for parameter endpoint"
        msg.asLeft[URL]
      case Right(endpointStr) =>
        Try(new URL(endpointStr)) match {
          case Success(url) => url.asRight[String]
          case Failure(ex)  => ex.getMessage.asLeft[URL]
        }
    }
  } yield ep
}

/** Enumeration of the different possible Endpoint states.
  */
private[endpoint] object EndpointStatus extends Enumeration {
  type EndpointStatus = String

  val ONLINE  = "online"
  val OFFLINE = "offline"

}
