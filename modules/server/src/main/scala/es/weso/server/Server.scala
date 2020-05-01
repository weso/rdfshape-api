package es.weso.server
import org.http4s._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, HSTS, Logger}
import es.weso.server.utils.Http4sUtils._
import org.log4s.getLogger
import cats.effect._
import cats.implicits._
import es.weso.quickstart.{HelloWorld, TestRoutes}
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext.global
import scala.util.Properties.envOrNone
import scala.concurrent.duration._

object Server {

  def routesService[F[_]: ConcurrentEffect](blocker: Blocker, client: Client[F])(implicit T: Timer[F], C: ContextShift[F]): HttpRoutes[F] =
//    HelloService[F](blocker).routes <+>
    CORS (
      SchemaService[F](blocker,client).routes <+>
      APIService[F](blocker, client).routes <+>
      ShExService[F](blocker,client).routes <+>
      ShapeMapService[F](blocker,client).routes <+>
      WikidataService[F](blocker, client).routes <+>
      EndpointService[F](blocker,client).routes
    ) <+>
    WebService[F](blocker).routes <+>
    DataWebService[F](blocker, client).routes <+>
    StaticService[F](blocker).routes <+>
    LinksService[F](blocker).routes
        
  def stream[F[_]: ConcurrentEffect](blocker:Blocker, port: Int, ip: String)(implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global)
      // .withConnectTimeout(3.minute)
      // .withIdleTimeout(3.minute)
      .withRequestTimeout(5.minute)
      .stream
      app = (
        // HelloService[F](blocker).routes
	      HSTS( routesService[F](blocker,client) )
      ).orNotFound
      // .orNotFound
      finalHttpApp = Logger.httpApp(true, false)(app)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(port,ip)
        .withIdleTimeout(10.minutes)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
    }.drain

}