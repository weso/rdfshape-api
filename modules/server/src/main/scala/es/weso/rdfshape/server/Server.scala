package es.weso.rdfshape.server

import cats.effect._
import cats.implicits._
import es.weso.rdfshape.server.Server._
import es.weso.rdfshape.server.api._
import es.weso.rdfshape.server.utils.error.SysUtils
import es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException
import es.weso.rdfshape.server.utils.secure.SSLHelper
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, Logger}
import org.http4s.{HttpApp, HttpRoutes}

import javax.net.ssl.SSLContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}
import scala.util.{Failure, Success, Try}

private class Server(
    val port: Int,
    val https: Boolean,
    val verbose: Boolean = defaultVerbose,
    val requestTimeout: Int = defaultRequestTimeout,
    val idleTimeout: Int = defaultIdleTimeout
) extends IOApp {
  
  override def run(args: List[String]): IO[ExitCode] = {
    println(s"""
        |Verbose mode ${if(verbose) "ON" else "OFF"}
        |Starting server on port $port...
        |Serving via ${if(https) "HTTPS" else "HTTP"}...
        |""".stripMargin)

    stream(getSslContext).compile.drain.as(ExitCode.Success)
  }

  private def getSslContext: Option[SSLContext] = {
    if(!https) return None

    Try(SSLHelper.getContext) match {
      case Success(context) => Some(context)
      case Failure(exception) =>
        val e = SSLContextCreationException(exception.getMessage, exception)
        SysUtils.fatalError(
          SysUtils.sslContextCreationError,
          e.getMessage
        )
        None
    }
  }

  private def stream(sslContext: Option[SSLContext]): Stream[IO, ExitCode] = {
    for {
      client <- BlazeClientBuilder[IO](global)
        .withRequestTimeout(requestTimeout.minute)
        .withIdleTimeout(idleTimeout.minute)
        .stream

      server: BlazeServerBuilder[IO] = http4sServer(client, sslContext)
      exitCode <- server.serve
    } yield exitCode
  }.drain

  private def createApp(client: Client[IO]): HttpApp[IO] = {
    val app = routesService(client).orNotFound
    Logger.httpApp(logHeaders = true, logBody = false)(app)
  }

  private def http4sServer(
      client: Client[IO],
      sslContext: Option[SSLContext] = None
  ): BlazeServerBuilder[IO] = {

    val baseServer = BlazeServerBuilder[IO](global)
      .bindHttp(port, ip)
      .withIdleTimeout(idleTimeout.minutes)
      .withResponseHeaderTimeout(requestTimeout.minute)
      .withHttpApp(createApp(client))

    sslContext match {
      case None          => baseServer
      case Some(context) => baseServer.withSslContext(context)
    }
  }
}

object Server {

  // Default server characteristics
  val defaultPort           = 8080
  val defaultHttps          = false
  val defaultRequestTimeout = 5
  val defaultIdleTimeout    = 10
  val defaultVerbose        = false

  // Always serve on localhost
  private val ip = "0.0.0.0"

  private val corsConfiguration = CORSConfig(
    anyOrigin = true,
    anyMethod = true,
    allowCredentials = true,
    maxAge = 1.day.toSeconds
  )

  // Act as a server factory
  def apply(port: Int): Unit = {
    apply(port, https = defaultHttps, verbose = defaultVerbose)
  }

  def apply(port: Int, https: Boolean): Unit = {
    apply(port, https, verbose = defaultVerbose)
  }

  def apply(port: Int, https: Boolean, verbose: Boolean): Unit = {
    val s = new Server(port, https, verbose = verbose)
    s.main(Array.empty[String])
  }

  // Linked routes
  private def routesService(client: Client[IO]): HttpRoutes[IO] =
    CORS(
      SchemaService(client).routes <+>
        APIService(client).routes <+>
        DataService(client).routes <+>
        ShExService(client).routes <+>
        ShapeMapService(client).routes <+>
        WikidataService(client).routes <+>
        EndpointService(client).routes <+>
        PermalinkService(client).routes <+>
        FetchService(client).routes,
      corsConfiguration
    )
}
