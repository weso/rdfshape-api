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

  val urlShortenerEndpoint = "https://cutt.ly/api/api.php"
  val urlShortenerAcceptCode = 7

  case class RequestData(domain: String, url: String)

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Query URL shortener API
    case GET -> Root / `api` / "permalink" / "generate" :?
      UrlParam(url) =>
      // Request the shortened URL
      try {
        val res = Http(urlShortenerEndpoint)
          .param("key", sys.env.getOrElse("CUTTLY_API_KEY", ""))
          .param("short", url)
          .asString

        val jsonRes = PJson.parse(res.body)
        // Return the short URL if possible
        if (res.code == 200 && (jsonRes \ "url" \ "status").isDefined
          && (jsonRes \ "url" \ "status").isDefined && (jsonRes \ "url" \ "status").as[Int] == urlShortenerAcceptCode)
            Ok(s"${(jsonRes \ "url" \ "shortLink").as[String]}")
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


