package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.format.dataFormats.{Compact, ShapeMapFormat}
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.ShapeMapTab
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.error.exceptions.JsonConversionException
import es.weso.rdfshape.server.utils.json.JsonUtils.maybeField
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.Json

/** Data class representing a ShapeMap and its current source.
  *
  * @note Invalid initial data is accepted, but may cause exceptions when operating with it (like converting to JSON).
  * @param shapeMapRaw          Shapemap raw text
  * @param shapeMapFormat       Shapemap format
  * @param targetShapeMapFormat Shapemap target format (only for conversion operations)
  * @param activeShapeMapTab    Active tab, used to know which source the shapemap comes from
  */
sealed case class ShapeMap private (
    shapeMapRaw: String,
    shapeMapFormat: ShapeMapFormat,
    targetShapeMapFormat: ShapeMapFormat,
    activeShapeMapTab: String
) {

  /** Inner shapemap structure of the data in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = {
    ShapeMapW
      .fromString(shapeMapRaw, shapeMapFormat.name) match {
      case Left(errorList) => Left(errorList.toList.mkString("\n"))
      case Right(shapeMap) => Right(shapeMap)
    }
  }

  /** JSON representation of this shapemap to be used in API responses
    *
    * @return JSON information of the shapemap (raw content, format, JSON structure) or an
    */
  @throws(classOf[JsonConversionException])
  lazy val shapeMapJson: Json = {
    innerShapeMap match {
      case Left(err) => throw JsonConversionException(err)
      case Right(dataShapeMap) =>
        Json.fromFields(
          maybeField("shapeMap", Some(shapeMapRaw), Json.fromString) ++
            maybeField(
              "shapeMapFormat",
              Some(shapeMapFormat),
              (format: ShapeMapFormat) => Json.fromString(format.name)
            ) ++
            maybeField(
              "shapeMapJson",
              Some(dataShapeMap.toJson),
              identity[Json]
            )
        )
    }

  }
}

private[api] object ShapeMap extends LazyLogging {

  /** Placeholder value used for the shapemap whenever an empty shapemap is issued/needed.
    */
  val emptyShapeMapValue = ""

  /** Default shapemap format used when no alternatives are present
    */
  private val defaultShapeMapFormat: ShapeMapFormat = Compact

  /** Given a request's parameters, try to extract a shapemap from them
    *
    * @param partsMap Request's parameters
    * @return Either the shapemap or an error message
    */
  def getShapeMap(
      partsMap: PartsMap
  ): IO[Either[String, ShapeMap]] = {
    for {
      // Get data sent in que query
      shapeMapStr  <- partsMap.optPartValue(ShapeMapTextParameter.name)
      shapeMapUrl  <- partsMap.optPartValue(ShapeMapUrlParameter.name)
      shapeMapFile <- partsMap.optPartValue(ShapeMapFileParameter.name)
      shapeMapFormat <- ShapeMapFormat.fromRequestParams(
        ShapeMapFormatParameter.name,
        partsMap
      )
      targetShapeMapFormat <- ShapeMapFormat.fromRequestParams(
        TargetShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue(
        ActiveShapeSourceTabParameter.name
      )

      _ = logger.debug(
        s"Getting ShapeMap from params. ShapeMap tab: $activeShapeMapTab"
      )

      // Create the shapemap depending on the client's selected method
      maybeShapeMap: Either[String, ShapeMap] = mkShapeMap(
        shapeMapStr,
        shapeMapUrl,
        shapeMapFile,
        shapeMapFormat,
        targetShapeMapFormat,
        activeShapeMapTab
      )

    } yield maybeShapeMap
  }

  /** Create a ShapeMap instance, given its source and format
    *
    * @param shapeMapStr          Optionally, the raw contents of the shapemap
    * @param shapeMapUrl          Optionally, the URL with the contents of the shapemap
    * @param shapeMapFile         Optionally, the file with the contents of the shapemap
    * @param shapeMapFormat       Optionally, the format of the shapemap
    * @param targetShapeMapFormat Optionally, the target format of the shapemap (for conversions)
    * @param activeShapeMapTab    Optionally, the indicator of the shapemap source (raw, url or file)
    * @return A new ShapeMap based on the given parameters
    */
  def mkShapeMap(
      shapeMapStr: Option[String],
      shapeMapUrl: Option[String],
      shapeMapFile: Option[String],
      shapeMapFormat: Option[ShapeMapFormat],
      targetShapeMapFormat: Option[ShapeMapFormat],
      activeShapeMapTab: Option[ShapeMapTab]
  ): Either[String, ShapeMap] = {
    // Confirm chosen formats
    val format =
      shapeMapFormat.getOrElse(ShapeMapFormat.defaultFormat)
    val targetFormat =
      targetShapeMapFormat.getOrElse(ShapeMapFormat.defaultFormat)

    // Create the shapemap depending on the client's selected method
    val maybeShapeMap: Either[String, ShapeMap] = activeShapeMapTab.getOrElse(
      ShapeMapSource.defaultActiveShapeMapTab
    ) match {
      case ShapeMapSource.TEXT =>
        shapeMapStr match {
          case None => Left("No value for the ShapeMap string")
          case Some(shapeMapRaw) =>
            Right(
              ShapeMap(
                shapeMapRaw,
                format,
                targetFormat,
                ShapeMapSource.TEXT
              )
            )
        }

      case ShapeMapSource.URL =>
        shapeMapUrl match {
          case None => Left(s"No value for the shapemap URL")
          case Some(url) =>
            getUrlContents(url) match {
              case Right(shapeMapRaw) =>
                Right(
                  ShapeMap(
                    shapeMapRaw,
                    format,
                    targetFormat,
                    ShapeMapSource.URL
                  )
                )
              case Left(err) => Left(err)
            }
        }
      case ShapeMapSource.FILE =>
        shapeMapFile match {
          case None => Left(s"No value for the shapemap file")
          case Some(shapeMapRaw) =>
            Right(
              ShapeMap(
                shapeMapRaw,
                format,
                targetFormat,
                ShapeMapSource.FILE
              )
            )
        }
      case other =>
        val msg = s"Unknown value for activeShapemapTab: $other"
        logger.warn(msg)
        Left(msg)
    }

    maybeShapeMap
  }

  /** @return Empty shapemap representation, with no inner data and all defaults
    */
  private def empty: ShapeMap =
    ShapeMap(
      shapeMapRaw = emptyShapeMapValue,
      shapeMapFormat = defaultShapeMapFormat,
      targetShapeMapFormat = defaultShapeMapFormat,
      activeShapeMapTab = ShapeMapSource.defaultActiveShapeMapTab
    )

}

/** Enumeration of the different possible ShapeMap sources sent by the client.
  * The source sent indicates the API if the shapemap was sent in raw text, as a URL
  * to be fetched or as a text file containing the shapemap.
  * In case the client submits the shapemap in several formats, the selected source will indicate the preferred one.
  */
private[api] object ShapeMapSource extends Enumeration {
  type ShapeMapTab = String

  val TEXT = "#shapeMapTextArea"
  val URL  = "#shapeMapUrl"
  val FILE = "#shapeMapFile"

  val defaultActiveShapeMapTab: ShapeMapTab = TEXT
}
