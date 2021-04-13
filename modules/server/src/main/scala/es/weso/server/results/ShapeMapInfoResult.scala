package es.weso.server.results

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.server.ApiHelper
import es.weso.server.helper._
import es.weso.utils.json.JsonUtilsServer._
import io.circe.Json
import es.weso.shapemaps._

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
