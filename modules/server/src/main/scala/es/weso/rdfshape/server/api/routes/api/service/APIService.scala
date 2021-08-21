package es.weso.rdfshape.server.api.routes.api.service

import cats.effect._
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

/** API service to handle multiple general tasks (server status, etc.)
  *
  * @param client HTTP4S client object
  */
class APIService(client: Client[IO]) extends Http4sDsl[IO] with ApiService {

  override val verb: String = ""

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "health" =>
      Ok("OK")
  }

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
