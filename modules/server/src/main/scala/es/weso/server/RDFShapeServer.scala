package es.weso.server

// import java.util.concurrent.Executors

// import org.http4s.util.{ExitCode, StreamApp}
import cats._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.effect._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import org.http4s.server.middleware.CORS
import org.http4s.server.staticcontent.FileService.Config
import org.log4s.getLogger
// import fs2.Stream
import scala.util.Properties.envOrNone
import cats.implicits._
import cats.effect._
import org.http4s.twirl._
import es.weso._
import org.http4s.server.staticcontent._

/*
class HelloService[F[_]](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  def routes(implicit timer: Timer[F]): HttpRoutes[F] =
    Router[F](
      "" -> staticRoutes.combineK(rootRoutes)  // TODO: <+> could be used instead of combineK but if gives an error
    )

  private val views: HttpRoutes[F] =
    fileService(Config(systemPath = "/static", blocker = blocker))

  def staticRoutes = resourceService[F](ResourceService.Config("/static", blocker))

  def rootRoutes(implicit timer: Timer[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {

      case GET -> Root / "hi" =>
        Ok(html.index())
    }

}

object HelloService {
 def apply[F[_]: Effect: ContextShift](blocker: Blocker): HelloService[F] =
    new HelloService[F](blocker)
}
*/

class RDFShapeServer[F[_]:ConcurrentEffect: Timer](host: String, port: Int)(implicit F: Effect[F], cs: ContextShift[F]) {
  private val logger = getLogger
  // private val pool = Executors.newCachedThreadPool()

  logger.info(s"Starting RDFShape on '$host:$port'")

    /* <+>
    APIService.apiService <+>
    WebService.webService */

  def routesService(blocker: Blocker): HttpRoutes[F] =
    CORS(WebService[F](blocker).routes)

/*  val service = routesService.local { req: Request[IO] =>
    val path = req.uri.path
    logger.debug(s"Request with path: ${req.remoteAddr.getOrElse("null")} -> ${req.method}: $path")
    req
  } */

/*  def build(): fs2.Stream[IO, ExitCode] =
    BlazeServerBuilder[IO].bindHttp(port, host)
      .mountService(service).
      serve */

  def httpApp(blocker: Blocker): HttpApp[F] =
    routesService(blocker).orNotFound

  def resource: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      server <- BlazeServerBuilder[F]
        .bindHttp(8080)
        .withHttpApp(httpApp(blocker))
        .resource
    } yield server

}

object RDFShapeServer extends IOApp {
  val ip = "0.0.0.0"
  val port = envOrNone("PORT") map (_.toInt) getOrElse (8080)
  // private[this] val logger = getLogger

  override def run(args: List[String]): IO[ExitCode]  =
    new RDFShapeServer[IO](ip,port).resource.use(_ => IO.never).as(ExitCode.Success)

}