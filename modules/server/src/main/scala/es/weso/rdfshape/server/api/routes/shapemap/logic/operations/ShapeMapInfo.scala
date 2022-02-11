package es.weso.rdfshape.server.api.routes.shapemap.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.routes.shapemap.logic.operations.ShapeMapInfo.ShapeMapInfoResult
import es.weso.rdfshape.server.utils.json.JsonUtils.prefixMap2JsonArray
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a shapeMap-information operation
  *
  * @param inputShapeMap ShapeMap used as input of the operation
  * @param result        [[ShapeMapInfoResult]] containing the resulting schema information
  */
final case class ShapeMapInfo private (
    override val inputShapeMap: ShapeMap,
    result: ShapeMapInfoResult
) extends ShapeMapOperation(ShapeMapInfo.successMessage, inputShapeMap)

private[api] object ShapeMapInfo extends LazyLogging {

  private val successMessage = "Well formed ShapeMap"

  /** Given an input ShapeMap, get information about it
    *
    * @param shapeMap Input shapeMap instance of any type
    * @return A [[ShapeMapInfo]] instance with the information of the input schema
    */

  def shapeMapInfo(shapeMap: ShapeMap): IO[ShapeMapInfo] = {
    val inner = shapeMap.innerShapeMap

    inner match {
      case Left(err) => IO.raiseError(new RuntimeException(err))
      case Right(shapeMapW) =>
        IO {
          ShapeMapInfo(
            inputShapeMap = shapeMap,
            result = ShapeMapInfoResult(
              shapeMap = shapeMap,
              model = shapeMapW,
              numberOfAssociations = shapeMapW.associations.length,
              nodesPrefixMap = shapeMapW.nodesPrefixMap,
              shapesPrefixMap = shapeMapW.shapesPrefixMap
            )
          )
        }
    }

  }

  /** Case class representing the results to be returned when performing a shapeMap-info operation
    *
    * @param shapeMap             ShapeMap operated on
    * @param model                The inner model of the associations in the shapeMap in use
    * @param numberOfAssociations Number of node-shape associations stated in the shapeMap
    * @param nodesPrefixMap       Prefix map for the nodes in the shapeMap
    * @param shapesPrefixMap      Prefix map for the shapes in the shapeMap
    */
  final case class ShapeMapInfoResult private (
      shapeMap: ShapeMap,
      model: ShapeMapW,
      numberOfAssociations: Int,
      nodesPrefixMap: PrefixMap,
      shapesPrefixMap: PrefixMap
  )

  /** JSON encoder for [[ShapeMapInfoResult]]
    */
  private implicit val encodeShapeMapInfoResult: Encoder[ShapeMapInfoResult] =
    (shapeMapInfoResult: ShapeMapInfoResult) =>
      Json.fromFields(
        List(
          (
            "numberOfAssociations",
            shapeMapInfoResult.numberOfAssociations.asJson
          ),
          ("format", shapeMapInfoResult.shapeMap.format.asJson),
          (
            "nodesPrefixMap",
            prefixMap2JsonArray(shapeMapInfoResult.nodesPrefixMap)
          ),
          (
            "shapesPrefixMap",
            prefixMap2JsonArray(shapeMapInfoResult.shapesPrefixMap)
          )
        )
      )

  /** JSON encoder for [[ShapeMapInfoResult]]
    */
  implicit val encodeShapeMapInfoOperation: Encoder[ShapeMapInfo] =
    (shapeMapInfo: ShapeMapInfo) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(shapeMapInfo.successMessage)),
          ("shapeMap", shapeMapInfo.inputShapeMap.asJson),
          ("result", shapeMapInfo.result.asJson)
        )
      )
}
