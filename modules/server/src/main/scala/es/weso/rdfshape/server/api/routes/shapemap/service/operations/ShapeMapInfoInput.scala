package es.weso.rdfshape.server.api.routes.shapemap.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.ShapeMapParameter
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

case class ShapeMapInfoInput(shapeMap: ShapeMap)

object ShapeMapInfoInput extends ServiceRouteOperation[ShapeMapInfoInput] {

  override implicit val decoder: Decoder[ShapeMapInfoInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeShapeMap <- cursor
          .downField(ShapeMapParameter.name)
          .as[Either[String, ShapeMap]]
      } yield maybeShapeMap.map(ShapeMapInfoInput(_))

      mapEitherToDecodeResult(decodeResult)
    }
}
