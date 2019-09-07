package es.weso.server

import java.net.URI

import cats.Applicative
import cats.data.EitherT
import cats.effect.{Effect, IO}
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.{RDFReader, RDFReasoner}
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import es.weso.rdf.nodes.IRI
import es.weso.server.Defaults._
import es.weso.server.helper.DataFormat
import io.circe.Json
import org.log4s.getLogger
import scalaj.http._

case class EndpointInfo(msg: String, status: Option[String] = None) {
  def asJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg)),
      ("status", Json.fromString(status.getOrElse("")))
    )
  )
}

case class EndpointParam(url: String) {

  def getEndpointAsRDFReader[F[_]:Applicative]: EitherT[F, String,RDFReader] =
    EitherT.fromEither[F](Endpoint.fromString(url))

  def getInfo[F[_]: Effect]: F[EndpointInfo] = {
    println(s"Obtaining info of endpoint $url")
    Effect[F].liftIO(IO {
      try {
        val response: HttpResponse[String] = Http(url).asString
        val statusLine = response.statusLine
        println(s"Response: $statusLine")
        EndpointInfo(msg = "OK", status = Some(statusLine))
      } catch {
        case e : Throwable =>
         EndpointInfo(msg =s"Excepton: ${e.getMessage}")
      }
    })
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
