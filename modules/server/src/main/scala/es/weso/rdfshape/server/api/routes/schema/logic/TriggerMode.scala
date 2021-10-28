package es.weso.rdfshape.server.api.routes.schema.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.shapemaps.{ShapeMap => ShapeMapW}

/** Data class representing a TriggerMode and its current source.
  *
  * @param triggerModeStr Trigger mode name
  * @param shapeMap       Inner shapemap associated to the TriggerMode
  */
sealed case class TriggerMode private (
    triggerModeStr: String,
    shapeMap: ShapeMap
) extends LazyLogging {

  /** Inner shapemap structure of the shapemap contained in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = shapeMap.innerShapeMap
}

private[api] object TriggerMode extends LazyLogging {

  /** Given a request's parameters, try to extract a TriggerMode instance from them
    *
    * @param partsMap Request's parameters
    * @return Either the trigger mode or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap
  ): IO[Either[String, TriggerMode]] = {
    for {
      // Get data sent in que query
      triggerMode   <- partsMap.optPartValue(TriggerModeParameter.name)
      paramShapemap <- partsMap.optPartValue(ShapeMapParameter.name)

      shapeMapFormat <- ShapeMapFormat.fromRequestParams(
        ShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue(
        ShapemapSourceParameter.name
      )

      // Get companion shapemap
      maybeShapeMap <- ShapeMap.mkShapeMap(
        paramShapemap,
        shapeMapFormat,
        None,
        activeShapeMapTab
      )

    } yield {
      maybeShapeMap.flatMap(sm => mkTriggerMode(triggerMode, sm))
    }

  }

  /** Create a TriggerMode instance, given its mode and shapemap
    *
    * @param triggerMode Optionally, the trigger mode name
    * @param shapeMap    Optionally, the inner shapemap associated to the TriggerMode
    * @return A new TriggerMode based on the given parameters
    */
  private def mkTriggerMode(
      triggerMode: Option[String],
      shapeMap: ShapeMap
  ): Either[String, TriggerMode] = {
    Right(
      TriggerMode(
        triggerModeStr = triggerMode.getOrElse(ApiDefaults.defaultTriggerMode),
        shapeMap = shapeMap
      )
    )

  }

}
