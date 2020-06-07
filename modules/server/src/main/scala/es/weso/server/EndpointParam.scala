package es.weso.server

import cats.data.EitherT
import cats.effect._
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.Endpoint
import io.circe.Json
// import scalaj.http._
import java.util.concurrent.Executors

import cats.effect.ContextShift
import es.weso.utils.IOUtils._
import org.http4s.client.{Client, JavaNetClientBuilder}

import scala.concurrent.ExecutionContext.global


case class EndpointInfo(msg: String, status: Option[String] = None) {
  def asJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg)),
      ("status", Json.fromString(status.getOrElse("")))
    )
  )
}

case class EndpointParam(url: String) {

  def getEndpointAsRDFReader[F[_]:Effect]: EitherT[F, String, RDFReader] =
    io2esf[RDFReader,F](Endpoint.fromString(url))

  def getInfo[F[_]: Effect]: F[EndpointInfo] = {
    println(s"Obtaining info of endpoint $url")
    Effect[F].liftIO({
        implicit val cs: ContextShift[IO] = IO.contextShift(global)
        implicit val timer: Timer[IO] = IO.timer(global)
        val blockingPool = Executors.newFixedThreadPool(5)
        val blocker = Blocker.liftExecutorService(blockingPool)
        val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create
        // val resolvedUri = baseUri.resolve(uri)
        //logger.info(s"Resolved: $resolvedUri")
        httpClient.expect[String](url).map(EndpointInfo(_))
    }
/*      try {
        val response: HttpResponse[String] = Http(url).asString
        val statusLine = response.statusLine
        println(s"Response: $statusLine")
        EndpointInfo(msg = "OK", status = Some(statusLine))
      } catch {
        case e : Throwable =>
         EndpointInfo(msg =s"Excepton: ${e.getMessage}")
      } */
    ) 
  }
}

object EndpointParam {
  private[server] def mkEndpoint[F[_]:Effect](partsMap: PartsMap[F]):
    EitherT[F, String, EndpointParam] = for {
    maybeStr <- EitherT.liftF[F, String, Option[String]] (partsMap.optPartValue("endpoint"))
    ep <- maybeStr match {
      case None => EitherT.leftT[F,EndpointParam](s"No value for param endpoint")
      case Some(str) => EitherT.rightT[F,String](EndpointParam(str))
    }
  } yield ep
}

