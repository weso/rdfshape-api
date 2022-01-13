package es.weso.rdfshape.server.api.routes.fetch.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.UrlParameter
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class FetchService() extends Http4sDsl[IO] with ApiService with LazyLogging {

  override val verb: String = "fetch"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Query a given URL and return the response.
      * Receives the URL to be queried:
      *  - url [String]: URL to be queried
      *    Returns the URL contents (response body)
      */
    case GET -> Root / `api` / `verb` :?
        UrlParameter(url) =>
      getUrlContents(url) match {
        case Left(err)      => errorResponseJson(err, InternalServerError)
        case Right(content) => Ok(content)
      }
  }

  case class RequestData(domain: String, url: String)
}

object FetchService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Fetch Service
    */
  def apply(client: Client[IO]): FetchService =
    new FetchService()
}