package es.weso.rdfshape.server.utils.networking

import com.typesafe.scalalogging.LazyLogging

import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

object NetworkingUtils extends LazyLogging {

  /** Error-safe way of obtaining the raw contents in a given URL
    *
    * @param urlString URL to be fetched (String representation)
    * @return Either the contents if the URL or an error message
    */
  def getUrlContents(urlString: String): Either[String, String] = {
    Try {
      val url = new URL(urlString)
      val src = Source.fromURL(url)
      val str = src.mkString
      src.close()
      str
    } match {
      case Success(urlContent) => Right(urlContent)
      case Failure(exception) =>
        val msg =
          s"Could not obtain data from url $urlString."
        logger.warn(s"$msg - ${exception.getMessage}")
        Left(msg)
    }
  }

}
