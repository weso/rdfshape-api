package es.weso.rdfshape.server.launcher

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import es.weso.rdfshape.server.launcher.Server.{ip, stream}
import es.weso.rdfshape.server.server._
import es.weso.rdfshape.server.utils.secure.SSLHelper
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}

import javax.net.ssl.SSLContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.postfixOps

private class Server(val port: Int, val verbose: Boolean) extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    println(s"Starting server on port $port")
    println(s"Verbose mode is ${if (verbose) "ON" else "OFF"}")
    stream(port, ip).compile.drain.as(ExitCode.Success)
  }
}

object Server {

  // Server on localhost
  private val ip = "0.0.0.0"

  // Act as a server factory
  def apply(port: Int): Unit = {
    apply(port, verbose = false)
  }
  def apply(port: Int, verbose: Boolean): Unit = {
    val s = new Server(port, verbose)
    s.main(Array.empty[String])
  }

  private def stream(port: Int, ip: String): Stream[IO, Nothing] = {

    for {
      client <- BlazeClientBuilder[IO](global)
        .withRequestTimeout(5.minute)
        .stream
      app          = routesService(client).orNotFound
      sslContext   = SSLHelper.getContext
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = false)(app)

      baseServer = BlazeServerBuilder[IO](global)
        .bindHttp(port, ip)
        .withIdleTimeout(10.minutes)
        .withHttpApp(finalHttpApp)

      // Use HTTPS only if an SSL context could be created.
      server =
        if(sslContext == SSLContext.getDefault) {
          println(s"Serving via HTTP")
          baseServer
        } else {
          println(s"Serving via HTTPS")
          baseServer
            .withSslContext(sslContext)
        }

      exitCode <- server.serve
    } yield exitCode
  }.drain

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
        FetchService(client).routes
    )
}
