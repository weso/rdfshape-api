package es.weso.rdfshape.server.api.routes.api

import cats.effect._
import es.weso.rdfshape.server.api.routes.ApiDefinitions._
import es.weso.rdfshape.server.api.routes.{ApiService, Defaults}
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent.resourceServiceBuilder
import org.log4s.getLogger

/** API service to handle multiple general tasks (server status, etc.)
  *
  * @param client HTTP4S client object
  */
class APIService(client: Client[IO]) extends Http4sDsl[IO] with ApiService {

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ GET -> Root / `api` / "health" =>
      Ok("OK")

  }
  private val relativeBase = Defaults.relativeBase
  private val logger       = getLogger
  private val swagger =
    resourceServiceBuilder[IO]("/swagger") // ResourceService.Config())

  private def errJson(msg: String): IO[Response[IO]] =
    Ok(Json.fromFields(List(("error", Json.fromString(msg)))))

}

object APIService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new API Service
    */
  def apply(client: Client[IO]): APIService =
    new APIService(client)
}
