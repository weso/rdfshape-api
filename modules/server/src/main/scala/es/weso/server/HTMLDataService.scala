package es.weso.server

import cats.effect._
import cats.implicits._
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.ApiHelper._
import results._
import es.weso.server.Defaults.{
  availableDataFormats,
  availableInferenceEngines,
  defaultActiveDataTab,
  defaultDataFormat,
  defaultInference
}
import es.weso.server.QueryParams._
import es.weso.server.helper.DataFormat
import es.weso.server.utils.Http4sUtils._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.log4s.getLogger

import scala.concurrent.duration._
import APIDefinitions._
import cats.Monad
import cats.data.EitherT
import es.weso.html
import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.IRI
import org.http4s.dsl.io.Ok
import es.weso.utils.IOUtils._

import scala.util.Try

class HTMLDataService(client: Client[IO]) extends Http4sDsl[IO] {

  private val relativeBase = Defaults.relativeBase
  private val logger       = getLogger

}

object HTMLDataService {
  def apply(client: Client[IO]): HTMLDataService = new HTMLDataService(client)
}
