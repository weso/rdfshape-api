package es.weso.rdfshape.server.api
import cats.effect._
import es.weso.rdfshape.server.api.APIDefinitions._
import es.weso.schema._
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class ShExService(client: Client[IO]) extends Http4sDsl[IO] {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "shEx" / "formats" =>
      val formats = Schemas.availableFormats
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)
  }
}

object ShExService {
  def apply(client: Client[IO]): ShExService =
    new ShExService(client)
}
