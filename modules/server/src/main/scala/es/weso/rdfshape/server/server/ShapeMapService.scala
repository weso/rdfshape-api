package es.weso.rdfshape.server.server

import cats.effect._
import es.weso.rdfshape.server.server.APIDefinitions._
import es.weso.rdfshape.server.server.ApiHelper._
import es.weso.rdfshape.server.server.results.ShapeMapInfoResult
import es.weso.shapemaps.ShapeMap
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart._

class ShapeMapService(client: Client[IO]) extends Http4sDsl[IO] {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "shapeMap" / "formats" =>
      val formats = ShapeMap.availableFormats
      val json    = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)

    case req @ POST -> Root / `api` / "shapeMap" / "info" =>
      req.decode[Multipart[IO]] { m =>
        println(s"ShapeMap/info")
        val partsMap = PartsMap(m.parts)
        val t: IO[(ShapeMap, ShapeMapParam)] =
          ShapeMapParam.mkShapeMap(partsMap)
        t.attempt.flatMap(
          _.fold(
            e => Ok(mkJsonErr(e.getMessage())),
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
  def apply(client: Client[IO]): ShapeMapService = new ShapeMapService(client)
}
