package es.weso.server

import cats.effect._
import es.weso.server.APIDefinitions._
import es.weso.server.QueryParams.UrlParam
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import scalaj.http.Http

class FetchService() extends Http4sDsl[IO] {

  case class RequestData(domain: String, url: String)
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Query URL and return the response
    case GET -> Root / `api` / "fetch" :?
      UrlParam(url) =>
      pprint.log(s"Fetching url: $url")
      try {
        val res = Http(url).asString
        if (res.isSuccess) {
          Ok(res.body)
        } else {
          InternalServerError("Could not fetch URL")
        }
      }
      catch {
        case _: Exception =>
          InternalServerError("Could not fetch URL")
      }
  }
}

object FetchService {
  def apply(client: Client[IO]): FetchService =
    new FetchService()
}
