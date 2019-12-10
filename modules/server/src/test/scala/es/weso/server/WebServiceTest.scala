package es.weso.server

import org.scalatest._
import org.scalatestplus.selenium.HtmlUnit
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
import scala.concurrent.ExecutionContext
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers



class WebServiceTest extends AnyFunSpec
  with Matchers
  with EitherValues
  with BeforeAndAfter
  with HtmlUnit {
  val ip = "0.0.0.0"
  val port = 8080
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val shaclexServer = new RDFShapeServer[IO](ip, port)
  var server: Server[IO] = null

  before {
    println(s"Before tests...starting server...")
    // val builder = BlazeBuilder[IO].bindHttp(port,"localhost").mountService(shaclexServer.service).start
    // server = builder.unsafeRunSync
  }

  after {
    println(s"After tests...closing server and browser...")
    // server.shutdown.unsafeRunSync
    close
    quit
  }

  // val host = s"http://localhost:$port"
  val host = s"http://weso.rdfshape.es"


 /* describe(s"Home page") {

    it(s"Should contain SHACLex") {
      go to (host)
      pageTitle should contain("SHACLex")
    }

  } */
}