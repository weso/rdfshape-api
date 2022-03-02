package es.weso.rdfshape.server.api.routes.api.service

import cats.effect._
import es.weso.rdfshape.server.api.routes.ApiService
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes

/** API service to handle multiple general tasks (server status, etc.)
  *
  * @param client HTTP4S client object
  */
class BaseService(client: Client[IO]) extends Http4sDsl[IO] with ApiService {

  override val verb: String = "base"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    GET / "health" |>> { () =>
      Ok("Healthy")
    }
  }
}

object BaseService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new API Service
    */
  def apply(client: Client[IO]): BaseService =
    new BaseService(client)
}
