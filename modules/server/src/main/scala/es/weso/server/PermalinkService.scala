package es.weso.server

import cats.effect._
import es.weso.server.APIDefinitions._
import es.weso.server.QueryParams.UrlParam
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import play.api.libs.json.{Json => PJson}
import scalaj.http.Http
import java.net.URL

class PermalinkService[F[_]]()(implicit F: Effect[F], cs: ContextShift[F])
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

        val responseJson = PJson.parse(res.body)
        // Check for a valid response from code
        if (res.code == 200 && (responseJson \ "url" \ "status").as[Int] == urlShortenerAcceptCode) {
          val url = new URL(s"${(responseJson \ "url" \ "shortLink").as[String]}").toURI
          Ok(url.toString)
        }
        else
          InternalServerError(url)
      }
      catch {
        case _: Exception =>
          InternalServerError(url)
      }
    }
}

object PermalinkService {
  def apply[F[_]: Effect: ContextShift](blocker: Blocker, client: Client[F]): PermalinkService[F] =
    new PermalinkService[F]()
}


