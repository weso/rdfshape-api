package es.weso.server
import cats.effect._
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.shapeMaps.{Status => ShapeMapStatus, _}
import io.circe.Json
import io.circe.parser._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Uri, Query => HQuery}
import org.scalatest.funspec._
import org.scalatest.matchers.should._
import es.weso.utils.test._

import scala.concurrent.ExecutionContext.global

class APIServiceTest extends AnyFunSpec with Matchers with JsonMatchers {


}
