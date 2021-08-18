package es.weso.rdfshape.server.api.routes.fetch

import cats.effect._
import es.weso.rdfshape.server.api.routes.ApiDefinitions._
import es.weso.rdfshape.server.api.routes.IncomingRequestParameters.UrlParam
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import scalaj.http.Http

class FetchService() extends Http4sDsl[IO] {

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Query URL and return the response
    case GET -> Root / `api` / "fetch" :?
        UrlParam(url) =>
      try {
        val res = Http(url).asString
        if(res.isSuccess) {
          Ok(res.body)
        } else {
          InternalServerError("Could not fetch URL")
        }
      } catch {
        case _: Exception =>
          InternalServerError("Could not fetch URL")
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
