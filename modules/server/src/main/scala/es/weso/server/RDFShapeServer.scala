package es.weso.server

import org.http4s._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import es.weso.server.utils.Http4sUtils._
import org.log4s.getLogger
import cats.effect._
import cats.implicits._
import es.weso.schemaInfer.Config
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent.ResourceService

import scala.concurrent.ExecutionContext.global
import scala.util.Properties.envOrNone


class HelloService[F[_]](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  def routes(implicit timer: Timer[F]): HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root =>
        Ok("Hi!")
    }
}

object HelloService {
 def apply[F[_]: Effect: ContextShift](blocker: Blocker): HelloService[F] =
    new HelloService[F](blocker)
}


class RDFShapeServer[F[_]:ConcurrentEffect: Timer](host: String, port: Int)(implicit F: Effect[F], cs: ContextShift[F]) {
  private val logger = getLogger
  // private val pool = Executors.newCachedThreadPool()

  logger.info(s"Starting RDFShape on '$host:$port'")

  def routesService(blocker: Blocker, client: Client[F]): HttpRoutes[F] =
    HelloService[F](blocker).routes
/*    CORS(
      WebService[F](blocker).routes <+>
      DataService[F](blocker, client).routes <+>
      WikidataService[F](blocker, client).routes <+>
      ShExService[F](blocker,client).routes <+>
      SchemaService[F](blocker,client).routes <+>
      ShapeMapService[F](blocker,client).routes <+>
      APIService[F](blocker, client).routes <+>
      EndpointService[F](blocker).routes <+>
      LinksService[F](blocker).routes
    ) */

/*  val service = routesService.local { req: Request[IO] =>
    val path = req.uri.path
    logger.debug(s"Request with path: ${req.remoteAddr.getOrElse("null")} -> ${req.method}: $path")
    req
  } */

/*  def build(): fs2.Stream[IO, ExitCode] =
    BlazeServerBuilder[IO].bindHttp(port, host)
      .mountService(service).
      serve */

  def httpApp(blocker: Blocker,
              client: Client[F]): HttpApp[F] =
    routesService(blocker, client).orNotFound

  def resource: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      client <- BlazeClientBuilder[F](global).resource
      server <- BlazeServerBuilder[F]
        .bindHttp(port)
        .withHttpApp(httpApp(blocker,mkClient(client)))
        .resource
    } yield server

}

object RDFShapeServer extends IOApp {
  private val ip = "0.0.0.0"
  private val port = envOrNone("PORT") map (_.toInt) getOrElse 8080
  println(s"ENV PORT ${System getenv "PORT"}")
  println(s"PORT: $port")

  override def run(args: List[String]): IO[ExitCode]  =
    new RDFShapeServer[IO](ip,port).resource.use(_ => IO.never).as(ExitCode.Success)

}