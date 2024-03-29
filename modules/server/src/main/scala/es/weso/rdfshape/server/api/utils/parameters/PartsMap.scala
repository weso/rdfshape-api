package es.weso.rdfshape.server.api.utils.parameters

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.format.Format
import fs2.text.utf8.decode
import org.http4s.multipart.Part

/** Data class containing a map of a request's parameters with the form (param name: param content)
  * The data contained in a request parameter is handled via the {@link Part} class of HTT4s and extracted with the class methods
  *
  * @param map Map with the request's parameters information
  */
case class PartsMap private (map: Map[String, Part[IO]]) {

  /** Shorthand for extracting boolean values from a request parameter
    *
    * @param key Parameter key
    * @return Optionally, the boolean translation of the contents of the parameter
    */
  def optPartValueBoolean(key: String): IO[Option[Boolean]] = for {
    maybeValue <- optPartValue(key)
  } yield maybeValue match {
    case Some("true")  => Some(true)
    case Some("false") => Some(false)
    case _             => None

  }

  /** Extract the value from a request parameter, decoding it and handling errors
    *
    * @param key Parameter key
    * @param alt Alternative value to be returned when parameter value
   *                    is missing
    * @return Optionally, the String contents of the parameter
    */
  def optPartValue(key: String, alt: Option[String] = None): IO[Option[String]] =
    map.get(key) match {
      case Some(part) =>
        part.body.through(decode).compile.foldMonoid.map(Some.apply)
      case None => IO.pure(None)
    }

  /** Shorthand for extracting values from a request parameter with an informational error message
    *
    * @param key Parameter key
    * @return Either the String contents of the parameter or an error message
    */
  def eitherPartValue(key: String): IO[Either[String, String]] = for {
    maybeValue <- optPartValue(key)
  } yield maybeValue match {
    case None =>
      Left(
        s"Not found value for key $key\nKeys available: ${map.keySet.mkString(",")}"
      )
    case Some(s) => Right(s)
  }
}

object PartsMap extends LazyLogging{

  /** Instantiate a new {@link PartsMap} given a list of the inner parts
    *
    * @param ps List of parts
    * @return A new Parts map containing mapping each part's name to its contents
    */
  def apply(ps: Vector[Part[IO]]): PartsMap = {
    PartsMap(ps.filter(_.name.isDefined).map(p => (p.name.get, p)).toMap)
  }

  /** Try to build a Format object from a request's parameters
    *
    * @param parameter    Name of the parameter with the format name
    * @param parameterMap Request parameters
    * @return Optionally, a new generic Format instance with the format 
    */
  def getFormat(
      parameter: String,
      parameterMap: PartsMap
  ): IO[Option[Format]] = {
    for {
      maybeFormatName <- parameterMap.optPartValue(parameter)
    } yield maybeFormatName match {
      case None =>
        logger.info(s"No valid format found for parameter \"$parameter\"")
        None
      case Some(formatNameParsed) =>
        logger.info(s"Format value \"$formatNameParsed\" found in parameter \"$parameter\"")
        Format.fromString(formatNameParsed).toOption
    }
  }

}
