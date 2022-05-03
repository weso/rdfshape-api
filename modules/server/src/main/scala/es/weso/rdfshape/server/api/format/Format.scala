package es.weso.rdfshape.server.api.format

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.format.dataFormats._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.MediaType
import org.ragna.comet.data.{DataFormat => DataFormatComet}

/** Generic interface for any format any data transmitted to/from the API may have
  */
trait Format {

  /** Format friendly name
    */
  val name: String

  /** Format mime type (e.g., application/json, image/png, etc.)
    */
  val mimeType: MediaType

  override def toString: String = name

  override def equals(otherFormat: Any): Boolean = {
    otherFormat match {
      case other: Format => name == other.name && mimeType == other.mimeType
      case _             => false
    }
  }

}

object Format extends FormatCompanion[Format] {

  override val default: Format = DataFormat.default

  // Should append all available formats in the future.
  // Currently, all formats are data formats.
  override val availableFormats: List[Format] =
    DataFormat.availableFormats // ++ futureFormats
}

/** Static utilities to be used with formats
  *
  * @tparam F Specific format type that we are handling
  */
trait FormatCompanion[F <: Format] extends LazyLogging {

  implicit class FormatOps(format: Format) {

    /** Given an RDFShape-domain data format, attempt to convert it to a data format
      * used in the streaming validation library
      *
      * @return The equivalent of the input format in the stream-validation
      *         library, if available
      */
    def toStreamingDataFormat: Either[String, DataFormatComet] =
      format match {
        case rdfFormat: RdfFormat =>
          rdfFormat match {
            case Turtle   => DataFormatComet.TURTLE.asRight
            case NTriples => DataFormatComet.NTRIPLES.asRight
            case Trig     => DataFormatComet.TRIG.asRight
            case JsonLd   => DataFormatComet.JSONLD.asRight
            case RdfXml   => DataFormatComet.RDFXML.asRight
            case RdfJson  => DataFormatComet.RDFJSON.asRight
            case _ =>
              Left(
                s"Data format '${format.name}' is not supported for stream validations"
              )
          }

      }
  }

  /** Default format to be used when none specified
    */
  val default: F

  /** List of all formats available for the current type of entity
    */
  val availableFormats: List[F]

  /** Format encoder. Forms a JSON object with the formats name and mimetype
    */
  implicit val encoder: Encoder[F] = (format: F) => {
    Json.obj(
      ("name", Json.fromString(format.name)),
      (
        "mimeType",
        Json.fromString(
          s"${format.mimeType.mainType}/${format.mimeType.subType}"
        )
      )
    )
  }

  /** Format decoder. Forms a Format instance from a given String, if the format name is valid.
    *
    * @note The decoder is simplified because the client normally sends the format name only, like:
    *       "format": "turtle"
    */
  implicit val decoder: Decoder[Either[String, F]] = (cursor: HCursor) =>
    for {
      formatStr <- cursor.value.as[String]
      format = fromString(formatStr)
    } yield format

  /** Simplified version of the standard decoder, ignoring the produced errors
    * and returning an option
    */
  implicit val decoderOption: Decoder[Option[F]] = decoder.map(_.toOption)

  /** Try to build a Format object from a request's parameters
    *
    * @param parameter    Name of the parameter with the format name
    * @param parameterMap Request parameters
    * @return Optionally, a new Format instance of type F with the format
    */
  def fromRequestParams(
      parameter: String = "format",
      parameterMap: PartsMap
  ): IO[Option[F]] = {
    for {
      maybeFormatName <- parameterMap.optPartValue(parameter)
    } yield maybeFormatName match {
      case None =>
        logger.info(s"No valid format found for parameter '$parameter'")
        None
      case Some(formatNameParsed) =>
        logger.info(
          s"Format value '$formatNameParsed' found in parameter '$parameter'"
        )
        fromString(formatNameParsed).toOption
    }
  }

  /** Given a format name, get its corresponding DataFormat object
    * DataFormat
    *
    * @param name String name of the format we require
    * @return the Format object with the format data (an error String if it does not exist)
    */
  def fromString(name: String): Either[String, F] = {
    if(name.isBlank) Right(default)
    else {
      formatsMap.get(name.toLowerCase) match {
        case None =>
          val errorMsg = mkErrorMessage(name)
          logger.error(errorMsg)
          Left(errorMsg)
        case Some(format) => Right(format)
      }
    }
  }

  /** Make a generic error message string to be used elsewhere
    *
    * @param name Name of the problematic format
    * @return Error message with format alternatives
    */
  def mkErrorMessage(name: String): String =
    s"Not found format: $name. Available formats: ${availableFormats.mkString(",")}"

  /** @return Map [String, Format] containing all formats while using their names as keys
    */
  def formatsMap: Map[String, F] = {
    def getFormatPairs(format: F): (String, F) =
      (format.name.toLowerCase(), format)

    availableFormats.map(getFormatPairs).toMap
  }
}
