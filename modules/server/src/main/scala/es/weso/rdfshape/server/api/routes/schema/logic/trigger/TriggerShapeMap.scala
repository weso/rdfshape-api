package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType.TriggerModeType
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{ShapeMapTrigger, ValidationTrigger}
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Data class representing a validation trigger enabled by a shapemap,
  * for ShEx validations.
  *
  * @param shapeMap Inner shapemap associated to the [[TriggerShapeMap()]]
  */
sealed case class TriggerShapeMap private (
    shapeMap: ShapeMap
) extends TriggerMode
    with LazyLogging {

  /** Inner shapemap structure of the shapemap contained in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = shapeMap.innerShapeMap

  override val triggerModeType: TriggerModeType = TriggerModeType.SHAPEMAP

  override def getValidationTrigger: Either[String, ValidationTrigger] =
    innerShapeMap.map(ShapeMapTrigger(_))
}

private[api] object TriggerShapeMap
    extends TriggerModeCompanion[TriggerShapeMap]
    with LazyLogging {

  /** Given a request's parameters, try to extract a TriggerMode instance from them
    *
    * @param partsMap Request's parameters
    * @return Either the trigger mode or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap
  ): IO[Either[String, TriggerShapeMap]] = {
    for {
      // Get companion shapemap from params
      maybeShapeMap <- ShapeMap.mkShapeMap(partsMap)

      // Create TriggerMode instance
      maybeTriggerMode = maybeShapeMap.map(shapeMap =>
        TriggerShapeMap(shapeMap)
      )

    } yield maybeTriggerMode
  }

  override implicit val encodeTriggerMode: Encoder[TriggerShapeMap] =
    (tsm: TriggerShapeMap) =>
      Json.obj(
        ("type", tsm.triggerModeType.asJson),
        ("shapeMap", tsm.shapeMap.asJson)
      )

  override implicit val decodeTriggerMode: Decoder[TriggerShapeMap] =
    (cursor: HCursor) =>
      for {
        shapeMap <- cursor.downField("shapeMap").as[ShapeMap]
        decoded = TriggerShapeMap(shapeMap)
      } yield decoded
}
