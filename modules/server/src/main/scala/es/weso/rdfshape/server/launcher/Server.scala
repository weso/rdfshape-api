package es.weso.rdfshape.server.launcher

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import es.weso.rdfshape.server.launcher.Server.{ip, routesService}
import es.weso.rdfshape.server.server._
import es.weso.rdfshape.server.utils.secure.SSLHelper
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, Logger}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

private class Server(val port: Int, val verbose: Boolean) extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    println(s"Starting server on port $port...")
    println(s"Verbose mode is ${if(verbose) "ON" else "OFF"}")
    stream().compile.drain.as(ExitCode.Success)
  }

  private def stream(): Stream[IO, Nothing] = {
    for {
      client <- BlazeClientBuilder[IO](global)
        .withRequestTimeout(5.minute)
        .stream
      app          = routesService(client).orNotFound
      sslContext   = Try(SSLHelper.getContext)
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = false)(app)

      baseServer = BlazeServerBuilder[IO](global)
        .bindHttp(port, ip)
        .withIdleTimeout(10.minutes)
        .withHttpApp(finalHttpApp)

      // Use HTTPS only if an SSL context could be created.
      server: BlazeServerBuilder[IO] = sslContext match {
        case Failure(e) =>
          System.err.println(
            s"Couldn't not create an SSL context. Reason: $e"
          )
          println("Serving via HTTP")
          baseServer
        case Success(ctx) =>
          ctx match {
            case Some(ctx) =>
              println("Serving via HTTPS")
              baseServer.withSslContext(ctx)
            case None =>
              println("Serving via HTTP")
              baseServer
          }
      }

      exitCode <- server.serve
    } yield exitCode
  }.drain
}

object Server {

  // Serve on localhost
  private val ip = "0.0.0.0"

  // Act as a server factory
  def apply(port: Int): Unit = {
    apply(port, verbose = false)
  }

  def apply(port: Int, verbose: Boolean): Unit = {
    val s = new Server(port, verbose)
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
      CORSConfig(
        anyOrigin = true,
        anyMethod = true,
        allowCredentials = true,
        maxAge = 1.day.toSeconds
      )
    )
}
