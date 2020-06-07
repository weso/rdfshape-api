package es.weso.server

import cats.effect._
import es.weso.server.APIDefinitions._
import es.weso.server.QueryParams.UrlParam
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import play.api.libs.json.{Json => PJson}
import scalaj.http.Http

class PermalinkService[F[_]](blocker: Blocker, client: Client[F])(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  val urlShortenerEndpoint = "https://api-ssl.bitly.com/v4/shorten"

  case class RequestData(domain: String, url: String)

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Query URL shortener API
    case GET -> Root / `api` / "permalink" / "generate" :?
      UrlParam(url) =>
      // Request the shortened URL
      try {
        val res = Http(urlShortenerEndpoint).postData(s"""{ "long_url": "$url" }""")
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("Authorization", "8e4c5fc54b280e5bc969fc0ef7a0f759317b7c64")
          .asString

        val jsonRes = PJson.parse(res.body)

        // Return the short URL if possible
        if (res.code >= 200 && res.code < 300 && (jsonRes \ "link").isDefined)
          Ok(s"${(jsonRes \ "link").as[String]}")
        else
          InternalServerError (url)
      }
      catch {
        case e: Exception =>
          InternalServerError (url)
      }
    }
}

object PermalinkService {
  def apply[F[_]: Effect: ContextShift](blocker: Blocker, client: Client[F]): PermalinkService[F] =
    new PermalinkService[F](blocker, client)
}


