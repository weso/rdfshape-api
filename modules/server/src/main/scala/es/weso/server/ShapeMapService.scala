package es.weso.server

import cats.effect._
import cats.data.EitherT
import es.weso.schema._
import es.weso.server.APIDefinitions._
import es.weso.shapeMaps.ShapeMap
import io.circe._
import org.http4s._
import org.http4s.implicits._
import org.http4s.multipart._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.log4s.getLogger
import es.weso.server.results.ShapeMapInfoResult

class ShapeMapService[F[_]:ConcurrentEffect: Timer](blocker: Blocker,
                                               client: Client[F])(implicit cs: ContextShift[F])
  extends Http4sDsl[F] {

  val routes = HttpRoutes.of[F] {

    case GET -> Root / `api` / "shapeMap" / "formats" => {
      val formats = ShapeMap.availableFormats
      val json = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)
    }

    case req@POST -> Root / `api` / "shapeMap" / "info" => {
      req.decode[Multipart[F]] { m =>
        println(s"ShapeMap/info")
        val partsMap = PartsMap(m.parts)
        val t: EitherT[F,String,(ShapeMap,ShapeMapParam)] = ShapeMapParam.mkShapeMap(partsMap)
        t.foldF(e => BadRequest(e), pair => {
          val (sm,smp) = pair
          val smi: ShapeMapInfoResult = ShapeMapInfoResult.fromShapeMap(smp.shapeMap,smp.optShapeMapFormat, sm)
          Ok(smi.toJson)
        })
      }
    }

/*    case req@GET -> Root / `api` / "shapeMap" / "info" :?
      OptShapeMapParam(optShapeMap) +&
      OptShapeMapURLParam(optShapeMapURL) +&
      ShapeMapFormatParam(maybeShapeMapFormat)  => Ok()

      val either: Either[String, Option[DataFormat]] = for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => errJson(str)
        case Right(optDataFormat) => {
          val dp =
            DataParam(optData, optDataURL, None, optEndpoint,
              optDataFormat, optDataFormat, optDataFormat,
              None, //no dataFormatFile
              optInference,
              None, optActiveDataTab)
          val (maybeStr, eitherRDF) = dp.getData(relativeBase)
          eitherRDF.fold(
            str => errJson(str),
            rdf => {
              Ok(dataInfo(rdf, maybeStr, optDataFormat))
            })
        }
      }
    } */

 }
}

object ShapeMapService {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](blocker: Blocker, client: Client[F]): ShapeMapService[F] =
    new ShapeMapService[F](blocker, client)
}



