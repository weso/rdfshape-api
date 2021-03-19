package es.weso.server
import cats.effect._
import cats.implicits._
import fs2.Stream
import javax.net.ssl.SSLContext
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Server {
  /*  def context[F[_]: Sync]: F[SSLContext] =
    SSLHelper.loadContextFromClasspath(SSLHelper.keystorePassword, SSLHelper.keyManagerPassword)

  def builder[F[_]: ConcurrentEffect: ContextShift: Timer](port: Int): F[BlazeServerBuilder[F]] =
   context.map { sslContext =>
     BlazeServerBuilder[F](global).bindHttp(port)
       .withSslContext(sslContext)
  } */

  def stream(port: Int, ip: String): Stream[IO, Nothing] = {

    for {
      client <- BlazeClientBuilder[IO](global)
        .withRequestTimeout(5.minute)
        //      .withSslContext(SSLContext.getDefault)
        .stream
      app = (
        // HelloService[F](blocker).routes
        // HSTS(
        routesService(client)
        // )
      ).orNotFound
      sslContext   = SSLHelper.getContext
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = false)(app)
      /* b <- Stream.eval(builder(port))
      exitCode <- b.withHttpApp(finalHttpApp).serve  */

      baseServer = BlazeServerBuilder[IO](global)
        .bindHttp(port, ip)
        .withIdleTimeout(10.minutes)
        .withHttpApp(finalHttpApp)

      // Use HTTPS only if an SSL context could be created.
      server = if (sslContext == SSLContext.getDefault) {
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

  /*  def context[F[_]: Sync] =
    ssl.loadContextFromClasspath(ssl.keystorePassword, ssl.keyManagerPassword)

  def builder[F[_]: ConcurrentEffect: ContextShift: Timer]: F[BlazeServerBuilder[F]] =
    context.map { sslContext =>
      BlazeServerBuilder[F](global)
        .bindHttp(8443)
        .withSslContext(sslContext)
    } */

  def routesService(client: Client[IO]): HttpRoutes[IO] =
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
    ) <+>
      WebService().routes <+>
      // DataWebService[F](blocker, client).routes <+>
      StaticService().routes <+>
      LinksService().routes

}
