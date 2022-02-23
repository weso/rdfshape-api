package es.weso.rdfshape.server

import cats.effect._
import cats.implicits.{catsSyntaxOptionId, toSemigroupKOps}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.Server._
import es.weso.rdfshape.server.api.routes.api.service.BaseService
import es.weso.rdfshape.server.api.routes.data.service.DataService
import es.weso.rdfshape.server.api.routes.endpoint.service.EndpointService
import es.weso.rdfshape.server.api.routes.fetch.service.FetchService
import es.weso.rdfshape.server.api.routes.permalink.service.PermalinkService
import es.weso.rdfshape.server.api.routes.schema.service.SchemaService
import es.weso.rdfshape.server.api.routes.shapemap.service.ShapeMapService
import es.weso.rdfshape.server.api.routes.wikibase.service.WikibaseService
import es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException
import es.weso.rdfshape.server.utils.error.{ExitCodes, SysUtils}
import es.weso.rdfshape.server.utils.secure.SSLHelper
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.rho.RhoMiddleware
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerMetadata}
import org.http4s.server.middleware.{CORS, CORSPolicy, Logger}
import org.http4s.{HttpApp, HttpRoutes}

import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/** Class representing the API's core server functionality, provided by the HTTP4s library.
  * A single Server is meant to be running simultaneously.
  * This class is private and closed to external usage modification, server initialization
  * is managed via its companion object.
  *
  * @param port           Port where the API server is exposed
  * @param https          Whether if the server should try to create a secure context or not, given the user's
  *                       environment configuration
  * @param requestTimeout Http4s application request timeout
  * @param idleTimeout    Http4s application idle timeout
  */
private class Server(
    val port: Int,
    val https: Boolean,
    val requestTimeout: Int = defaultRequestTimeout,
    val idleTimeout: Int = defaultIdleTimeout
) extends IOApp
    with LazyLogging {

  /** Start running the server, using the configuration stored in the instance attributes
    *
    * @param args Arguments passed to the IOApp. Should be an empty list since the arguments have been processed beforehand.
    * @return Application's exit code
    */
  override def run(args: List[String]): IO[ExitCode] = {
    println(s"""
         |Starting server on port $port...
         |Serving via ${if(https) "HTTPS" else "HTTP"}...
         |""".stripMargin)
    stream(getSslContext).compile.drain.as(ExitCode.Success)
  }

  /** Create an instance of a secure SSLContext for the application.
    *
    * @return None if no HTTPS is required; an SSLContext if HTTPS is required and the context could be created
    * @see {@link es.weso.rdfshape.server.utils.secure.SSLHelper}
    * @note If an error occurs creating the SSLContext, program termination will occur
    */
  private def getSslContext: Option[SSLContext] = {
    if(!https) return None

    Try(SSLHelper.getContext) match {
      case Success(context) => Some(context)
      case Failure(exception) =>
        val e = SSLContextCreationException(exception.getMessage, exception)
        SysUtils.fatalError(
          ExitCodes.SSL_CONTEXT_CREATION_ERROR,
          e.getMessage
        )
        None
    }
  }

  /** Start an infinite stream in charge of processing incoming requests
    *
    * @param sslContext SSLContext used by the application (may be empty)
    * @return Application's exit code
    */
  private def stream(sslContext: Option[SSLContext]): Stream[IO, ExitCode] = {
    for {
      client <- BlazeClientBuilder[IO]
        .withRequestTimeout(requestTimeout.minute)
        .withIdleTimeout(idleTimeout.minute)
        .stream

      server: BlazeServerBuilder[IO] = http4sServer(client, sslContext)
      exitCode <- server.serve
    } yield exitCode
  }.drain

  /** Create the final http4s server
    *
    * @param client     Http4s' client in charge of the application
    * @param sslContext SSLContext used by the application
    * @return The final http4s server, with the proper application and SSLContext bound
    */
  private def http4sServer(
      client: Client[IO],
      sslContext: Option[SSLContext] = None
  ): BlazeServerBuilder[IO] = {

    val baseServer = BlazeServerBuilder[IO]
      .bindHttp(port, ip)
      .withIdleTimeout(idleTimeout.minutes)
      .withResponseHeaderTimeout(requestTimeout.minute)
      .withHttpApp(createApp(client))

    sslContext match {
      case None          => baseServer
      case Some(context) => baseServer.withSslContext(context)
    }
  }

  /** Create an http4s application object
    *
    * @param client Http4s' client in charge of the application
    * @return Http4s' application with the given client and a request-logging middleware
    */
  private def createApp(client: Client[IO]): HttpApp[IO] = {
    val app = routesService(client).orNotFound
    // Http4s logger middleware settings
    Logger.httpApp(logHeaders = true, logBody = false)(app)
  }
}

/** Static utilities to aid when creating the Server, as when as for managing Server creation
  */
object Server {

  // Default server characteristics
  /** Application's default port, used if none is specified
    */
  val defaultPort = 8080

  /** Application's default HTTPS requirement, used if none is specified
    */
  val defaultHttps = false

  /** Application's default request timeout, used if none is specified
    */
  val defaultRequestTimeout = 5

  /** Application's default idle timeout, used if none is specified
    */
  val defaultIdleTimeout = 10

  /** Application's default port used if none is specified
    */
  val defaultVerbosity = 0

  // Always serve on localhost
  /** Application's default host IP address, equal to serving on localhost
    */
  private val ip = "0.0.0.0"

  /** Application's CORS configuration
    */
  private val corsConfiguration: CORSPolicy =
    CORS.policy.withAllowOriginAll.withAllowMethodsAll
      .withMaxAge(new FiniteDuration(1, TimeUnit.DAYS))

  // Act as a server factory

  /** Apply method, used as a factory for Server objects
    *
    * @param port Port where the API server will be exposed
    */
  def apply(port: Int): Unit = {
    apply(port, https = defaultHttps)
  }

  /** Apply method, used as a factory for Server objects
    *
    * @param port  Port where the API server will be exposed
    * @param https Whether if the server will try to create a secure context or not
    */
  def apply(port: Int, https: Boolean): Unit = {
    val s = new Server(port, https)
    s.main(Array.empty[String])
  }

  /** Configure the http4s application to use the specified sources as API routes
    */
  private def routesService(client: Client[IO]): HttpRoutes[IO] = {
    val allRoutes =
      (BaseService(client).routes and PermalinkService(client).routes)
        .toRoutes(swaggerMiddleware) <+>
        DataService(client).routes <+>
        SchemaService(client).routes <+>
        ShapeMapService(client).routes <+>
        WikibaseService(client).routes <+>
        EndpointService(client).routes <+>
        FetchService(client).routes
    corsConfiguration.apply(allRoutes)
  }

  /** All route functions composed into one
    * @note <+> is part of cats syntax for composing functions
    */
//  def allRoutes(client: Client[IO]): HttpRoutes[IO] =
//    BaseService(client).routes <+>
//      DataService(client).routes <+>
//      SchemaService(client).routes <+>
//      ShapeMapService(client).routes <+>
//      WikibaseService(client).routes <+>
//      EndpointService(client).routes <+>
//      PermalinkService(client).routes <+>
//      FetchService(client).routes

  /** Swagger middleware to transform the RhoService into HttpService
    * with an attached Swagger definition
    * Includes the base API spec
    * @note We use the default API paths; therefore swagger files are in
    *       /swagger.(json|yml)
    */
  lazy val swaggerMiddleware: RhoMiddleware[IO] =
    ioSwagger.createRhoMiddleware(
      swaggerFormats = DefaultSwaggerFormats,
      swaggerMetadata = SwaggerMetadata(
        apiInfo = Info(
          title = "RDFShape API",
          version =
            buildinfo.BuildInfo.version, // programmatically get project version
          description =
            ("RDFShape is web API for semantic data analysis and validation" +
              "implemented in Scala using http4s (https://http4s.org/)").some,
          contact = Contact(
            name = "WESO Research group",
            email = "info@weso.es".some,
            url = "https://www.weso.es/#contact".some
          ).some,
          license = License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
          ).some
        ),
        host = "api.rdfshape.weso.es".some,
        basePath = "/api/".some,
        schemes = List(Scheme.HTTP, Scheme.HTTPS),
        consumes = List("multipart/form-data"),
        produces = List("text/plain; charset=utf-8", "application/json"),
        tags = List(Tag(name = "api", description = "RDFShape REST API".some))
      )
    )
}
