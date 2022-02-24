package es.weso.rdfshape.server.api.routes.shapemap.logic

import cats.effect.IO
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.ShapeMapSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.util.Try

/** Data class representing a ShapeMap and its current source.
  *
  * @note Invalid initial data is accepted, but may cause exceptions when operating with it (like converting to JSON).
  * @param content         Shapemap contents, as received before being processed depending on the [[source]]
  * @param nodesPrefixMap  Prefix mappings of the data referenced in the shapemap
  * @param shapesPrefixMap Prefix mappings of the ShEx schema referenced in the shapemap
  * @param format          Shapemap format
  * @param source          Active source, used to know which source the shapemap comes from
  */
sealed case class ShapeMap private (
    private val content: String,
    private val nodesPrefixMap: PrefixMap = PrefixMap.empty,
    private val shapesPrefixMap: PrefixMap = PrefixMap.empty,
    format: ShapeMapFormat,
    source: ShapeMapSource
) extends LazyLogging {

  // Non empty content 
  assume(!content.isBlank, "Could not build the shapeMap from empty data")
  // Valid source
  assume(ShapeMapSource.values.exists(_ equalsIgnoreCase source),
    s"Unknown shapemap source: \"$source\"")

  /** Given the (user input) for the shapeMap and its source, fetch the shapeMap contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Optionally, a String containing the final text of the shapeMap query
    */
  lazy val fetchedContents: Either[String, String] = 
    if (source equalsIgnoreCase ShapeMapSource.URL)
      getUrlContents(content)
    // Text or file  
    else Right(content)
  

  assume(fetchedContents.isRight,
    fetchedContents.left.getOrElse("Unknown error"))
  
  /** Inner shapemap structure of the data in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = {
    if (raw.isBlank) "Cannot extract the ShapeMap from an empty instance".asLeft
    else ShapeMapW
      .fromString(
        raw,
        format.name,
        base = None,
        nodesPrefixMap,
        shapesPrefixMap
      ) match {
      case Left(errorList) => errorList.toList.mkString("\n").asLeft
      case Right(shapeMap) => shapeMap.asRight
    }
  }
  /**
   * Raw shapeMap value, i.e.: the text forming the shapeMap
   *
   * @note It is safely extracted fromm [[fetchedContents]] after asserting 
   *       the contents are right
   */
  val raw: String = fetchedContents.toOption.get
}

private[api] object ShapeMap extends LazyLogging {

  /** JSON representation of this shapemap to be used in API responses
    *
    * @return JSON information of the shapemap (raw content, format, JSON structure)
    */
  implicit val encoder: Encoder[ShapeMap] =
    (shapeMap: ShapeMap) =>
      Json.obj(
        ("shapeMap", shapeMap.raw.asJson),
        ("format", shapeMap.format.asJson),
        ("model", shapeMap.innerShapeMap.toOption.map(_.toJson).asJson)
      )

  /** Decode JSON into [[ShapeMap]] instances
    *
    * @return [[ShapeMap]] instance created from JSON data
    */
  implicit val decoder: Decoder[Either[String, ShapeMap]] =
    (cursor: HCursor) => {
      val shapeMapData = for {
        content <- cursor
          .downField(ContentParameter.name)
          .as[String]
          .map(_.trim)

        format <- cursor
          .downField(FormatParameter.name)
          .as[ShapeMapFormat]

        source <- cursor
          .downField(SourceParameter.name)
          .as[ShapeMapSource]

      } yield (content, format, source)

      shapeMapData.map {
        /* Destructure and try to build the object, catch the exception as error
         * message if needed */
        case (content, format, source) =>
          Try {
            ShapeMap(
              content,
              nodesPrefixMap = PrefixMap.empty,
              shapesPrefixMap = PrefixMap.empty,
              format,
              source
            )
          }.toEither.leftMap(err =>
            s"Could not build the ShapeMap from user data:\n ${err.getMessage}"
          )

      }
    }

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
        content = paramShapeMap.getOrElse(""),
        nodesPrefixMap = nodesPrefixMap.getOrElse(PrefixMap.empty),
        shapesPrefixMap = shapesPrefixMap.getOrElse(PrefixMap.empty),
        format = paramFormat.getOrElse(ApiDefaults.defaultShapeMapFormat),
        source = paramSource.getOrElse(ShapeMapSource.default)
      )
    } yield shapeMap.innerShapeMap.map(_ => shapeMap)
  }
}
