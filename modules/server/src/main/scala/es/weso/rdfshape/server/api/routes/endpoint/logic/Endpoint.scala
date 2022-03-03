package es.weso.rdfshape.server.api.routes.endpoint.logic

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.{Endpoint => EndpointJena}
import es.weso.rdfshape.server.api.routes.endpoint.logic.EndpointStatus.{
  EndpointStatus,
  OFFLINE,
  ONLINE
}
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.utils.IOUtils.{ESIO, io2es}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import java.net.URL

/** Data class representing an endpoint
  *
  * @param response    Message attached to the information/returned by the endpoint
  * @param status Status of the endpoint
  */
sealed case class Endpoint(response: String, status: EndpointStatus)

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

  /** Encoder [[Endpoint]] => [[Json]]
    */
  implicit val encoder: Encoder[Endpoint] =
    (endpoint: Endpoint) =>
      Json.obj(
        ("online", (endpoint.status == ONLINE).asJson),
        ("response", endpoint.response.asJson)
      )
}

/** Enumeration of the different possible Endpoint states.
  */
private[endpoint] object EndpointStatus extends Enumeration {
  type EndpointStatus = String

  val ONLINE  = "online"
  val OFFLINE = "offline"

}
