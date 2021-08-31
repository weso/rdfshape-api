package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.shapemaps.{Compact, ShapeMapFormat, ShapeMap => ShapeMapW}

/** Data class representing a ShapeMap and its current source
  *
  * @param shapeMap             Shapemap raw text
  * @param shapeMapFormat       Shapemap format
  * @param targetShapeMapFormat Shapemap target format
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
  val innerShapeMap: Either[String, ShapeMapW] = {
    ShapeMapW
      .fromString(shapeMap, shapeMapFormat.name) match {
      case Left(errorList) => Left(errorList.toList.mkString("\n"))
      case Right(sm)       => Right(sm)
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
      optShapeMapFormat <- getShapeMapFormat(
        ShapeMapFormatParameter.name,
        partsMap
      )
      optTargetShapeMapFormat <- getShapeMapFormat(
        TargetShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue(
        ActiveShapeMapTabParameter.name
      )

      _ = logger.debug(
        s"Getting ShapeMap from params. ShapeMap tab: $activeShapeMapTab"
      )

      // Get the shapemap formats or use the defaults
      shapeMapFormat = optShapeMapFormat.getOrElse(defaultShapeMapFormat)
      targetShapeMapFormat = optTargetShapeMapFormat.getOrElse(
        defaultShapeMapFormat
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

  /** Given a list of query parameters and a parameter name, try to create a ShapeMapFormat instance from the format name contained in the parameter
    *
    * @param name     Query parameter containing the format name
    * @param partsMap Query parameters
    * @return Optionally, a ShapeMapFormat instance corresponding to the shapemap format specified in the query parameters
    */
  private def getShapeMapFormat(
      name: String,
      partsMap: PartsMap
  ): IO[Option[ShapeMapFormat]] =
    for {
      maybeFormat <- partsMap.optPartValue(name)
    } yield maybeFormat match {
      case None => None
      case Some(str) =>
        ShapeMapFormat
          .fromString(str)
          .fold(
            err => {
              logger.error(s"Unsupported shapeMapFormat: $str ($err)")
              None
            },
            format => Some(format)
          )
    }

  /** Empty shapemap representation, with no inner data and all defaults
    *
    * @return
    */
  def empty: ShapeMap =
    ShapeMap(
      emptyShapeMapValue,
      defaultShapeMapFormat,
      defaultShapeMapFormat,
      ShapeMapTab.defaultActiveShapeMapTab
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
