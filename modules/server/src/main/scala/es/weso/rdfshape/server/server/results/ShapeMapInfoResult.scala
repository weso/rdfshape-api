package es.weso.rdfshape.server.server.results

import es.weso.shapemaps._
import es.weso.rdfshape.server.utils.json.JsonUtilsServer._
import io.circe.Json

case class ShapeMapInfoResult private (
    msg: String,
    shapeMap: Option[String],
    shapeMapFormat: Option[ShapeMapFormat],
    shapeMapJson: Option[Json]
) {
  def toJson: Json = {
    Json.fromFields(
      List(("msg", Json.fromString(msg))) ++
        maybeField(shapeMap, "shapeMap", Json.fromString) ++
        maybeField(
          shapeMapFormat,
          "shapeMapFormat",
          (sf: ShapeMapFormat) => Json.fromString(sf.name)
        ) ++
        maybeField(shapeMapJson, "shapeMapJson", identity[Json])
    )
  }

}

object ShapeMapInfoResult {
  def fromMsg(msg: String): ShapeMapInfoResult =
    ShapeMapInfoResult(msg, None, None, None)
  def fromShapeMap(
      shapeMapStr: Option[String],
      shapeMapFormat: Option[ShapeMapFormat],
      shapeMap: ShapeMap
  ): ShapeMapInfoResult =
    ShapeMapInfoResult(
      "Well formed Shape Map",
      shapeMapStr,
      shapeMapFormat,
      Some(shapeMap.toJson)
    )
}
