package es.weso.rdfshape.server.api.routes.shapemap.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.shapemap.logic.operations.ShapeMapInfo
import es.weso.rdfshape.server.api.routes.shapemap.service.operations.ShapeMapInfoInput
import io.circe._
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._

/** API service to handle shapemap-related operations
  *
  * @param client HTTP4S client object
  */
class ShapeMapService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "shapemap"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    "GET an array with the accepted ShapeMap formats" **
      GET / `verb` / "formats" |>> {
        val formats = ApiDefinitions.availableShapeMapFormats
        val json    = Json.fromValues(formats.map(f => Json.fromString(f.name)))
        Ok(json)
      }

    "Obtain information about a ShapeMap: number of associations, prefixes, model" **
      POST / `verb` / "info" ^ jsonOf[IO, ShapeMapInfoInput] |>> {
        body: ShapeMapInfoInput =>
          ShapeMapInfo
            .shapeMapInfo(body.shapeMap)
            .flatMap(info => Ok(info.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))
      }
  }

}

object ShapeMapService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new ShapeMap Service
    */
  def apply(client: Client[IO]): ShapeMapService = new ShapeMapService(client)
}
