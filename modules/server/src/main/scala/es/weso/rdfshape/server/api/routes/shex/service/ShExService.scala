package es.weso.rdfshape.server.api.routes.shex.service

import cats.effect._
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.schema._
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class ShExService(client: Client[IO]) extends Http4sDsl[IO] {

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "shEx" / "formats" =>
      val formats = Schemas.availableFormats
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)
  }
}

object ShExService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new ShEx Service
    */
  def apply(client: Client[IO]): ShExService =
    new ShExService(client)
}
