package es.weso.server

import cats.effect._
import es.weso.server.APIDefinitions._
import es.weso.server.QueryParams.UrlParam
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import scalaj.http.Http

class FetchService[F[_]]()(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  case class RequestData(domain: String, url: String)
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

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
  def apply[F[_]: Effect: ContextShift](blocker: Blocker, client: Client[F]): FetchService[F] =
    new FetchService[F]()
}
