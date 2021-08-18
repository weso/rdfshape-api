package es.weso.rdfshape.server.api.routes.shapemap

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.results.ShapeMapInfoResult
import es.weso.rdfshape.server.api.routes.ApiDefinitions._
import es.weso.rdfshape.server.api.routes.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.responseJson
import es.weso.shapemaps.ShapeMap
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart._

/** API service to handle shapemap-related operations
  *
  * @param client HTTP4S client object
  */
class ShapeMapService(client: Client[IO])
    extends Http4sDsl[IO]
    with LazyLogging {

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "shapeMap" / "formats" =>
      val formats = ShapeMap.availableFormats
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)

    case req @ POST -> Root / `api` / "shapeMap" / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val t: IO[(ShapeMap, ShapeMapParam)] =
          ShapeMapParam.mkShapeMap(partsMap)
        t.attempt.flatMap(
          _.fold(
            e => responseJson(e.getMessage, BadRequest),
            pair => {
              val (sm, smp) = pair
              val smi: ShapeMapInfoResult = ShapeMapInfoResult.fromShapeMap(
                smp.shapeMap,
                smp.optShapeMapFormat,
                sm
              )
              Ok(smi.toJson)
            }
          )
        )
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
