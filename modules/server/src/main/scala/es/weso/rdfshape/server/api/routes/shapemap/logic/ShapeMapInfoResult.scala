package es.weso.rdfshape.server.api.routes.shapemap.logic

import es.weso.rdfshape.server.utils.json.JsonUtils._
import es.weso.shapemaps._
import io.circe.Json

/** Data class representing the output of a ShapeMapInfo operation
  *
  * @param msg            Output informational message after processing. Used in case of error.
  * @param shapeMap       Input shapemap
  * @param shapeMapFormat Input shapemap format
  * @param shapeMapJson   Output shapemap (JSON representation)
  */
case class ShapeMapInfoResult private (
    msg: String,
    shapeMap: Option[String],
    shapeMapFormat: Option[ShapeMapFormat],
    shapeMapJson: Option[Json]
) {

  /** Convert a result to its JSON representation
    *
    * @return JSON information of the shapemap result
    */
  def toJson: Json = {
    Json.fromFields(
      List(("message", Json.fromString(msg))) ++
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

  /** Message attached to the result when created successfully
    */
  val successMessage = "Well formed ShapeMap"

  /** @param msg Error message contained in the result
    * @return A ShapeMapInfoResult consisting of a single error message and no data
    */
  def fromMsg(msg: String): ShapeMapInfoResult =
    ShapeMapInfoResult(msg, None, None, None)

  /** @return A ShapeMapInfoResult, given all the parameters needed to build it (shapemap, formats, etc.)
    */
  def fromShapeMap(
      shapeMapStr: Option[String],
      shapeMapFormat: Option[ShapeMapFormat],
      shapeMap: ShapeMap
  ): ShapeMapInfoResult =
    ShapeMapInfoResult(
      successMessage,
      shapeMapStr,
      shapeMapFormat,
      Some(shapeMap.toJson)
    )
}
