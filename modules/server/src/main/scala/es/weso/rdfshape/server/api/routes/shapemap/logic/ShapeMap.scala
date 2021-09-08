package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.format.{Compact, ShapeMapFormat}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.error.exceptions.JsonConversionException
import es.weso.rdfshape.server.utils.json.JsonUtils.maybeField
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.Json

/** Data class representing a ShapeMap and its current source.
  * @note Invalid initial data is accepted, but may cause exceptions when operating with it (like converting to JSON).
  *
  * @param shapeMap             Shapemap raw text
  * @param shapeMapFormat       Shapemap format
  * @param targetShapeMapFormat Shapemap target format (only present in conversion operations)
  * @param activeShapeMapTab    Active tab, used to know which source the shapemap comes from
  */
sealed case class ShapeMap private (
    shapeMap: String,
    shapeMapFormat: ShapeMapFormat,
    targetShapeMapFormat: ShapeMapFormat,
    activeShapeMapTab: String
) {

  /** Construct the inner shapemap structure from the data in this class
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = {
    ShapeMapW
      .fromString(shapeMap, shapeMapFormat.name) match {
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
          maybeField("shapeMap", Some(shapeMap), Json.fromString) ++
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
  private val emptyShapeMapValue = ""

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
      shapeMapFormat <- getShapeMapFormat(
        ShapeMapFormatParameter.name,
        partsMap
      )
      targetShapeMapFormat <- getShapeMapFormat(
        TargetShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue(
        ActiveShapeMapTabParameter.name
      )

      _ = logger.debug(
        s"Getting ShapeMap from params. ShapeMap tab: $activeShapeMapTab"
      )

      // Create the shapemap depending on the client's selected method
      maybeShapeMap: Either[String, ShapeMap] = activeShapeMapTab.getOrElse(
        ShapeMapTab.defaultActiveShapeMapTab
      ) match {
        case ShapeMapTab.TEXT =>
          shapeMapStr match {
            case None => Left("No value for the ShapeMap string")
            case Some(shapeMapRaw) =>
              Right(
                ShapeMap(
                  shapeMapRaw,
                  shapeMapFormat,
                  targetShapeMapFormat,
                  ShapeMapTab.TEXT
                )
              )
          }

        case ShapeMapTab.URL =>
          shapeMapUrl match {
            case None => Left(s"No value for the shapemap URL")
            case Some(url) =>
              getUrlContents(url) match {
                case Right(shapeMapRaw) =>
                  Right(
                    ShapeMap(
                      shapeMapRaw,
                      shapeMapFormat,
                      targetShapeMapFormat,
                      ShapeMapTab.URL
                    )
                  )
                case Left(err) => Left(err)
              }
          }
        case ShapeMapTab.FILE =>
          shapeMapFile match {
            case None => Left(s"No value for the shapemap file")
            case Some(shapeMapRaw) =>
              Right(
                ShapeMap(
                  shapeMapRaw,
                  shapeMapFormat,
                  targetShapeMapFormat,
                  ShapeMapTab.FILE
                )
              )
          }
        case other =>
          val msg = s"Unknown value for activeShapemapTab: $other"
          logger.warn(msg)
          Left(msg)
      }

    } yield maybeShapeMap
  }

  /** Try to build a {@link es.weso.rdfshape.server.api.format.ShapeMapFormat} object from a request's parameters
    *
    * @param parameter    Name of the parameter with the format name
    * @param parameterMap Request parameters
    * @return The ShapeMap format found or the default one
    */
  private def getShapeMapFormat(
      parameter: String,
      parameterMap: PartsMap
  ): IO[ShapeMapFormat] = {
    for {
      maybeFormat <- PartsMap.getFormat(parameter, parameterMap)
    } yield maybeFormat match {
      case None         => ShapeMapFormat.defaultFormat
      case Some(format) => new ShapeMapFormat(format)
    }

  }

  /** Empty shapemap representation, with no inner data and all defaults
    *
    * @return
    */
  def empty: ShapeMap =
    ShapeMap(
      shapeMap = emptyShapeMapValue,
      shapeMapFormat = defaultShapeMapFormat,
      targetShapeMapFormat = defaultShapeMapFormat,
      activeShapeMapTab = ShapeMapTab.defaultActiveShapeMapTab
    )

}

/** Enumeration of the different possible ShapeMap tabs sent by the client.
  * The tab sent indicates the API if the shapemap was sent in raw text, as a URL
  * to be fetched or as a text file containing the shapemap.
  * In case the client submits the shapemap in several formats, the selected tab will indicate the preferred one.
  */
private[this] object ShapeMapTab extends Enumeration {
  type ShapeMapTab = String

  val TEXT = "#shapeMapTextArea"
  val URL  = "#shapeMapUrl"
  val FILE = "#shapeMapFile"

  val defaultActiveShapeMapTab: ShapeMapTab = TEXT
}
