package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.{
  ShapeMapSource,
  defaultShapeMapSource
}
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
  * @param shapeMapPre          Shapemap contents, as received before being processed depending on the [[shapeMapSource]]
  * @param shapeMapFormat       Shapemap format
  * @param targetShapeMapFormat Optionally, the shapemap target format (only for conversion operations)
  * @param shapeMapSource       Active source, used to know which source the shapemap comes from
  */
sealed case class ShapeMap private (
    private val shapeMapPre: Option[String],
    shapeMapFormat: ShapeMapFormat,
    targetShapeMapFormat: Option[ShapeMapFormat],
    shapeMapSource: String
) extends LazyLogging {

  /** Given the (user input) for the shapeMap and its source, fetch the shapeMap contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Optionally, a String containing the final text of the shapeMap query
    */
  lazy val rawShapeMap: Option[String] = shapeMapSource match {
    case ShapeMapSource.TEXT | ShapeMapSource.FILE =>
      shapeMapPre
    case ShapeMapSource.URL =>
      shapeMapPre.flatMap(getUrlContents(_).toOption)

    case other =>
      logger.warn(s"Unknown value for activeQueryTab: $other")
      None
  }

  /** Inner shapemap structure of the data in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = {
    rawShapeMap match {
      case Some(shapeMapStr) =>
        ShapeMapW
          .fromString(shapeMapStr, shapeMapFormat.name) match {
          case Left(errorList) => Left(errorList.toList.mkString("\n"))
          case Right(shapeMap) => Right(shapeMap)
        }
      case None => Left("Cannot extract the ShapeMap from an empty instance")
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
          maybeField("shapeMap", rawShapeMap, Json.fromString) ++
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
  private val emptyShapeMap =
    ShapeMap(
      shapeMapPre = None,
      shapeMapFormat = ApiDefaults.defaultShapeMapFormat,
      targetShapeMapFormat = None,
      shapeMapSource = ShapeMapSource.defaultShapeMapSource
    )

  /** Given a request's parameters, try to extract a shapemap from them
    *
    * @param partsMap Request's parameters
    * @return Either the shapemap or an error message
    */
  def mkShapeMap(
      partsMap: PartsMap
  ): IO[Either[String, ShapeMap]] = {
    for {
      // Get data sent in que query
      paramShapemap <- partsMap.optPartValue(ShapeMapParameter.name)
      shapeMapFormat <- ShapeMapFormat.fromRequestParams(
        ShapeMapFormatParameter.name,
        partsMap
      )
      targetShapeMapFormat <- ShapeMapFormat.fromRequestParams(
        TargetShapeMapFormatParameter.name,
        partsMap
      )
      activeShapeMapSource <- partsMap.optPartValue(
        ShapemapSourceParameter.name
      )

      _ = logger.debug(
        s"Getting ShapeMap from params. ShapeMap tab: $activeShapeMapSource"
      )

      // Create the shapemap depending on the client's selected method
      maybeShapeMap <- mkShapeMap(
        paramShapemap,
        shapeMapFormat,
        targetShapeMapFormat,
        activeShapeMapSource
      )

    } yield maybeShapeMap
  }

  /** Create a ShapeMap instance, given its source and format
    *
    * @param optShapeMapData         Optionally, the contents of the shapemap
    * @param optShapeMapFormat       Optionally, the format of the shapemap
    * @param optTargetShapeMapFormat Optionally, the target format of the shapemap (for conversions)
    * @param optShapeMapSource       Optionally, the indicator of the shapemap source (raw, url or file)
    * @return A new ShapeMap based on the given parameters
    */
  private[api] def mkShapeMap(
      optShapeMapData: Option[String],
      optShapeMapFormat: Option[ShapeMapFormat],
      optTargetShapeMapFormat: Option[ShapeMapFormat],
      optShapeMapSource: Option[ShapeMapSource]
  ): IO[Either[String, ShapeMap]] =
    for {
      shapeMap <- IO {
        ShapeMap(
          shapeMapPre = optShapeMapData,
          shapeMapFormat =
            optShapeMapFormat.getOrElse(ApiDefaults.defaultShapeMapFormat),
          targetShapeMapFormat = optTargetShapeMapFormat,
          shapeMapSource = optShapeMapSource.getOrElse(defaultShapeMapSource)
        )
      }

      result = shapeMap.rawShapeMap match {
        case Some(_) => Right(shapeMap)
        case None    => Left("Could not build the shapeMap")
      }
    } yield result
}
