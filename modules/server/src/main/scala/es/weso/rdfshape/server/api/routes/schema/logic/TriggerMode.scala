package es.weso.rdfshape.server.api.routes.schema.service

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
  * @param shapeMap    Inner shapemap associated to the TriggerMode
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
  def getTriggerModeParam(
      partsMap: PartsMap
  ): IO[Either[String, TriggerMode]] = {
    for {
      // Get data sent in que query
      triggerMode  <- partsMap.optPartValue(TriggerModeParameter.name)
      shapeMapStr  <- partsMap.optPartValue(ShapeMapTextParameter.name)
      shapeMapUrl  <- partsMap.optPartValue(ShapeMapUrlParameter.name)
      shapeMapFile <- partsMap.optPartValue(ShapeMapFileParameter.name)

      shapeMapFormat <- ShapeMapFormat.fromRequestParams(
        ShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue(
        ActiveShapeMapTabParameter.name
      )

      // Get companion shapemap
      maybeShapeMap = ShapeMap.mkShapeMap(
        shapeMapStr,
        shapeMapUrl,
        shapeMapFile,
        shapeMapFormat,
        None,
        activeShapeMapTab
      )

    } yield {
      maybeShapeMap.map(shapeMap =>
        TriggerMode(
          triggerModeStr =
            triggerMode.getOrElse(ApiDefaults.defaultTriggerMode),
          shapeMap = shapeMap
        )
      )
    }

  }

}
