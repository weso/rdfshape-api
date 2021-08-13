package es.weso.rdfshape.server.api.format

import com.typesafe.scalalogging.LazyLogging
import org.http4s.MediaType

/** Generic interface for any format any data transmitted to/from the API may have
  */
trait Format {

  /** Format friendly name
    */
  val name: String

  /** Format mime type (e.g., application/json, image/png, etc.)
    */
  val mimeType: MediaType

  override def toString: String = s"Format $name"

}

trait FormatCompanion[F <: Format] extends LazyLogging {

  /** Default format to be used when none specified
    */
  val defaultFormat: F

  /** List of all formats available for the current type of entity
    */
  val availableFormats: List[F]

  /** Given a format name, get its corresponding DataFormat object
    * DataFormat
    *
    * @param name String name of the format we require
    * @return the DataFormat object with the format data (an error String if it does not exist)
    */
  def fromString(name: String): Either[String, F] = {
    if(name.isBlank) Right(defaultFormat)
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
