package es.weso.rdfshape.server.api.routes.shapemap.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.format.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap.getShapeMap
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart._

import scala.util.{Failure, Success, Try}

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

    /** Returns a JSON array with the accepted shapeMap formats.
      */
    case GET -> Root / `api` / `verb` / "formats" =>
      val formats = ShapeMapFormat.availableFormats.map(_.name)
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)

    /** Obtain information about a shapeMap.
      * Receives a JSON object with the input shapeMap information:
      *  - shapeMap [String]: Raw shapemap data
      *  - shapeMapUrl [String]: Url containing the shapemap
      *  - shapeMapFile [File Object]: File containing the shapemap
      *  - shapeMapFormat [String]: Format of the shapeMap
      *  - activeShapeMapTab [String]: Identifies the source of the shapeMap (raw, URL, file...)
      *    Returns a JSON object with the shapeMap information:
      *    - shapeMap [String]: Input shapeMap string
      *    - shapeMapFormat [String]: Input shapeMap format
      *    - shapeMapJson [Array]: Array of the elements in the shapeMap
      *        - node [String]: Referenced node
      *        - shape [String]: Target shape for the node
      */
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
                  // Success creating the inner ShapeMap instance from the data.
                  // Try to get JSON representation
                  case Right(_) =>
                    Try {
                      shapeMap.shapeMapJson
                    } match {
                      case Failure(exc) =>
                        errorResponseJson(exc.getMessage, InternalServerError)
                      case Success(json) => Ok(json)
                    }
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
