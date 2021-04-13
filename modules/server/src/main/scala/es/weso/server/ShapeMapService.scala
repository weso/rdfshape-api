package es.weso.server

import ApiHelper._
import cats.effect._
import cats.data.EitherT
import es.weso.schema._
import es.weso.server.APIDefinitions._
import es.weso.shapemaps.ShapeMap
import io.circe._
import org.http4s._
import org.http4s.implicits._
import org.http4s.multipart._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.log4s.getLogger
import es.weso.server.results.ShapeMapInfoResult

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

    /* case req@GET -> Root / `api` / "shapeMap" / "info" :?
     * OptShapeMapParam(optShapeMap) +& OptShapeMapURLParam(optShapeMapURL) +&
     * ShapeMapFormatParam(maybeShapeMapFormat) => Ok()
     *
     * val either: Either[String, Option[DataFormat]] = for { df <-
     * maybeDataFormat.map(DataFormat.fromString(_)).sequence } yield df
     *
     * either match { case Left(str) => errJson(str) case Right(optDataFormat)
     * => { val dp =
     * DataParam(optData, optDataURL, None, optEndpoint, optDataFormat,
     * optDataFormat, optDataFormat, None, //no dataFormatFile optInference,
     * None, optActiveDataTab) val (maybeStr, eitherRDF) =
     * dp.getData(relativeBase) eitherRDF.fold( str => errJson(str), rdf => {
     * Ok(dataInfo(rdf, maybeStr, optDataFormat)) }) } } } */

  }

}

object ShapeMapService {
  def apply(client: Client[IO]): ShapeMapService = new ShapeMapService(client)
}
