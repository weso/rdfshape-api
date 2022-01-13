package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.{
  ShapeMapSource,
  defaultShapeMapSource
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Data class representing a ShapeMap and its current source.
  *
  * @note Invalid initial data is accepted, but may cause exceptions when operating with it (like converting to JSON).
  * @param shapeMapPre     Shapemap contents, as received before being processed depending on the [[source]]
  * @param nodesPrefixMap  Prefix mappings of the data referenced in the shapemap
  * @param shapesPrefixMap Prefix mappings of the ShEx schema referenced in the shapemap
  * @param format          Shapemap format
  * @param source          Active source, used to know which source the shapemap comes from
  */
sealed case class ShapeMap private (
    private val shapeMapPre: Option[String],
    private val nodesPrefixMap: PrefixMap = PrefixMap.empty,
    private val shapesPrefixMap: PrefixMap = PrefixMap.empty,
    format: ShapeMapFormat,
    source: ShapeMapSource
) extends LazyLogging {

  /** Given the (user input) for the shapeMap and its source, fetch the shapeMap contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Optionally, a String containing the final text of the shapeMap query
    */
  lazy val rawShapeMap: Option[String] = source match {
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
    rawShapeMap.map(_.trim) match {
      case None | Some("") =>
        Left("Cannot extract the ShapeMap from an empty instance")
      case Some(shapeMapStr) =>
        println(nodesPrefixMap.pm)
        ShapeMapW
          .fromString(
            shapeMapStr,
            format.name,
            base = None,
            nodesPrefixMap,
            shapesPrefixMap
          ) match {
          case Left(errorList) => Left(errorList.toList.mkString("\n"))
          case Right(shapeMap) => Right(shapeMap)
        }
    }
  }
}

private[api] object ShapeMap extends LazyLogging {

  /** Placeholder value used for the shapemap whenever an empty shapemap is issued/needed.
    */
  val emptyShapeMap: ShapeMap =
    ShapeMap(
      shapeMapPre = None,
      format = ApiDefaults.defaultShapeMapFormat,
      source = ShapeMapSource.defaultShapeMapSource
    )

  /** JSON representation of this shapemap to be used in API responses
    *
    * @return JSON information of the shapemap (raw content, format, JSON structure)
    */
  implicit val encodeShapeMap: Encoder[ShapeMap] =
    (shapeMap: ShapeMap) =>
      Json.obj(
        ("shapeMap", shapeMap.rawShapeMap.asJson),
        ("format", shapeMap.format.asJson),
        ("model", shapeMap.innerShapeMap.toOption.map(_.toJson).asJson)
      )

  /** Decode JSON into [[ShapeMap]] instances
    *
    * @return [[ShapeMap]] instance created from JSON data
    */
  implicit val decodeShapeMap: Decoder[ShapeMap] =
    (cursor: HCursor) =>
      for {
        shapeMap <- cursor.downField("shapeMap").as[Option[String]]

        shapeMapFormat <- cursor
          .downField("shapeMapFormat")
          .as[ShapeMapFormat]

        shapeMapSource <- cursor
          .downField("shapeMapSource")
          .as[ShapeMapSource]
          .orElse(Right(ShapeMapSource.defaultShapeMapSource))

        decoded = ShapeMap.emptyShapeMap.copy(
          shapeMapPre = shapeMap,
          format = shapeMapFormat,
          source = shapeMapSource
        )

      } yield decoded

  /** Given a request's parameters, try to extract a shapemap from them
    *
    * @param partsMap Request's parameters
    * @return Either the shapemap or an error message
    */
  def mkShapeMap(
      partsMap: PartsMap,
      nodesPrefixMap: Option[PrefixMap] = None,
      shapesPrefixMap: Option[PrefixMap] = None
  ): IO[Either[String, ShapeMap]] = {
    for {
      // Get data sent in que query
      paramShapeMap <- partsMap.optPartValue(ShapeMapParameter.name)

      paramFormat <- ShapeMapFormat.fromRequestParams(
        ShapeMapFormatParameter.name,
        partsMap
      )

      paramSource <- partsMap.optPartValue(
        ShapemapSourceParameter.name
      )

      _ = logger.debug(
        s"Getting ShapeMap from params. ShapeMap tab: $paramSource"
      )

      // Create the shapemap instance
      shapeMap = ShapeMap(
        shapeMapPre = paramShapeMap,
        nodesPrefixMap = nodesPrefixMap.getOrElse(PrefixMap.empty),
        shapesPrefixMap = shapesPrefixMap.getOrElse(PrefixMap.empty),
        format = paramFormat.getOrElse(ApiDefaults.defaultShapeMapFormat),
        source = paramSource.getOrElse(defaultShapeMapSource)
      )
    } yield shapeMap.innerShapeMap.map(_ => shapeMap)
  }
}
