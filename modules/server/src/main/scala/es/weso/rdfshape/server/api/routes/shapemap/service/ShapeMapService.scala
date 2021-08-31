package es.weso.rdfshape.server.api.routes.shapemap.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap.getShapeMap
import es.weso.rdfshape.server.api.routes.shapemap.logic.{
  ShapeMap,
  ShapeMapInfoResult
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
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
    with ApiService
    with LazyLogging {

  override val verb: String = "shapeMap"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / `verb` / "formats" =>
      val formats = ShapeMapW.availableFormats
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)

    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        val maybeShapeMap: IO[Either[String, ShapeMap]] = getShapeMap(partsMap)
        maybeShapeMap.attempt.flatMap(
          _.fold(
            // General exception
            e => errorResponseJson(e.getMessage, InternalServerError),
            {
              // Error parsing the ShapeMap information sent
              case Left(errorStr) => errorResponseJson(errorStr, BadRequest)
              // Success parsing the ShapeMap information sent
              case Right(shapeMap) =>
                shapeMap.innerShapeMap match {
                  // Error creating the inner ShapeMap instance from the data
                  case Left(errorStr) =>
                    errorResponseJson(errorStr, InternalServerError)
                  // Success creating the inner ShapeMap instance from the data
                  case Right(innerSm) =>
                    val shapeMapInfo: ShapeMapInfoResult =
                      ShapeMapInfoResult.fromShapeMap(
                        Some(shapeMap.shapeMap),
                        Some(shapeMap.shapeMapFormat),
                        innerSm
                      )
                    Ok(shapeMapInfo.toJson)
                }
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
