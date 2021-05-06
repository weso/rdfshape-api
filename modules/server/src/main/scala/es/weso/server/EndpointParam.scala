package es.weso.server

import cats.data.EitherT
import cats.effect._
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.Endpoint
import io.circe.Json
// import scalaj.http._
import es.weso.utils.IOUtils._
import org.http4s.client.Client

case class EndpointInfo(msg: String, status: Option[String] = None) {
  def asJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg)),
      ("status", Json.fromString(status.getOrElse("")))
    )
  )
}

case class EndpointParam(url: String) {

  def getEndpointAsRDFReader: ESIO[RDFReader] =
    io2es(Endpoint.fromString(url))
//     io2es[RDFReader,F](Endpoint.fromString(url))

  def getInfo(client: Client[IO]): IO[EndpointInfo] = {
    IO.println(s"Obtaining info of endpoint $url") *>
      // Effect[F].liftIO({
      // implicit val cs: ContextShift[IO] = IO.contextShift(global)
      // implicit val timer: Timer[IO] = IO.timer(global)
      // val blockingPool = Executors.newFixedThreadPool(5)
      // val blocker = Blocker.liftExecutorService(blockingPool)
      // val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create
      // val resolvedUri = baseUri.resolve(uri)
      //logger.info(s"Resolved: $resolvedUri")
      client.expect[String](url).map(EndpointInfo(_))
    // }
    /* try { val response: HttpResponse[String] = Http(url).asString val
     * statusLine = response.statusLine println(s"Response: $statusLine")
     * EndpointInfo(msg = "OK", status = Some(statusLine)) } catch { case e :
     * Throwable => EndpointInfo(msg =s"Excepton: ${e.getMessage}") } */
    // )
  }
}

object EndpointParam {

  private[server] def mkEndpoint(
      partsMap: PartsMap
  ): EitherT[IO, String, EndpointParam] = for {
    maybeStr <- EitherT.liftF[IO, String, Option[String]](
      partsMap.optPartValue("endpoint")
    )
    ep <- maybeStr match {
      case None =>
        EitherT.leftT[IO, EndpointParam](s"No value for param endpoint")
      case Some(str) => EitherT.rightT[IO, String](EndpointParam(str))
    }
  } yield ep
}
