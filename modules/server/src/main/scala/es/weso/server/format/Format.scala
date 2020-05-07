package es.weso.server.format

import org.http4s.MediaType
import org.http4s.MediaType._

trait Format {
  val name: String
  val mimeType: MediaType


  def availableFormats[F <: Format]: List[F]
  def default[F<:Format]: F

  override def toString: String = s"Format $name"

  def fromString[F <: Format](name: String): Either[String,F] =
    if (name == "") Right(default)
    else {
    formatsMap.get(name.toLowerCase) match {
      case None => Left(s"Not found format: $name. Available formats: ${availableFormats.mkString(",")}")
      case Some(df) => Right(df)
    }
  }

  def formatsMap[F <: Format]: Map[String, F] = {
    def toPair(f: F): (String, F) = (f.name.toLowerCase(), f)   
    availableFormats.map(toPair).toMap
  }

}

