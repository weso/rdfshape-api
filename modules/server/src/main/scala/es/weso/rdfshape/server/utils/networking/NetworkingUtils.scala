package es.weso.rdfshape.server.utils.networking

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.Server
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect

import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object NetworkingUtils extends LazyLogging {

  /** Http4s functional client used to fetch data contained in URLs
    * It has rich functionality and its tuned to follow redirects
    */
  val httpClient: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO]
      .withRequestTimeout(Server.defaultRequestTimeout.minute)
      .withIdleTimeout(Server.defaultIdleTimeout.minute)
      .resource
      .map(FollowRedirect(3)(_))

  /** Error-safe way of obtaining the raw contents in a given URL
    *
    * @param urlString URL to be fetched (String representation)
    * @return Either the contents if the URL or an error message
    */
  def getUrlContents(urlString: String): Either[String, String] = {
    Try {
      val url = new URL(urlString)
      getUrlContents(url)
    } match {
      case Failure(exception) => Left(exception.getMessage)
      case Success(value)     => value
    }
  }

  def getUrlContents(url: URL): Either[String, String] = {
    val strResponse =
      httpClient
        .use(client =>
          // We use unsafe because we trust the java URL class syntax
          client.expect[String](Uri.unsafeFromString(url.toString))
        )
    Try {
      strResponse.unsafeRunSync()
    } match {
      case Success(urlContent) => Right(urlContent)
      case Failure(exception) =>
        val msg =
          s"Could not obtain data from $url."
        logger.warn(s"$msg - ${exception.getMessage}")
        Left(msg)
    }
  }

}
