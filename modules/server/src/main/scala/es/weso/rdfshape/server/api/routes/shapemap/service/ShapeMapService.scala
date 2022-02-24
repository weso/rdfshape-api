package es.weso.rdfshape.server.api.routes.shapemap.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.routes.shapemap.logic.operations.ShapeMapInfo
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import io.circe._
import io.circe.syntax.EncoderOps
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

  override val verb: String = "shapemap"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Returns a JSON array with the accepted shapeMap formats.
      */
    case GET -> Root / `api` / `verb` / "formats" =>
      val formats = ApiDefinitions.availableShapeMapFormats
      val json    = Json.fromValues(formats.map(f => Json.fromString(f.name)))
      Ok(json)

    /** Obtain information about a shapeMap.
      * Receives a JSON object with the input shapeMap information:
      *  - shapeMap [String]: Raw shapemap data
      *  - shapeMapFormat [String]: Format of the shapeMap
      *  - shapeMapSource [String]: Identifies the source of the shapeMap (raw, URL, file...)
      * Returns a JSON object with the query inputs and results (see [[ShapeMapInfo.encodeShapeMapInfoOperation]]).
      */
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        for {
          // Get the schema from the partsMap
          eitherShapeMap <- ShapeMap.mkShapeMap(
            partsMap
          )
          response <- eitherShapeMap.fold(
            // If there was an error parsing the schema, return it
            err => errorResponseJson(err, InternalServerError),
            // Else, try and compute the schema info
            shapeMap =>
              ShapeMapInfo
                .shapeMapInfo(shapeMap)
                .flatMap(info => Ok(info.asJson))
                .handleErrorWith(err =>
                  errorResponseJson(err.getMessage, InternalServerError)
                )
          )

        } yield response
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
