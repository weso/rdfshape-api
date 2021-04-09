package es.weso.server
import cats.effect._
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.shapemaps.{Status => ShapeMapStatus, _}
import io.circe.Json
import io.circe.parser._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Uri, Query => HQuery}
import es.weso.utils.test._
import org.http4s.Uri.{Path => UriPath}
import scala.concurrent.ExecutionContext.global
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import fs2._
import org.http4s.ember.client.EmberClientBuilder

class TestService(client: Client[IO]) extends Http4sDsl[IO] {

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "hi" => {
      val json = Json.fromString("hello")
      Ok(json)
    }
  }
}
object TestService {
  def apply(client: Client[IO]): TestService =
    new TestService(client)
}


class TestHttp4sTest extends CatsEffectSuite {

 val clientFixture: Fixture[Client[IO]] = ResourceSuiteLocalFixture(
    "client", EmberClientBuilder.default[IO].build
  )  

 override def munitFixtures = List(clientFixture)  

 def runReq(req: Request[IO],routes: HttpRoutes[IO]): IO[Response[IO]] =
    routes.orNotFound(req)
 

 test("Hi 42") {
    IO(42).map(n => assertEquals(n,42))
  }

 def parseBody(body: EntityBody[IO]): IO[String] = {
   body.through(text.utf8Decode).compile.toList.map(_.mkString)
 }

 def checkRequest(
   request: Request[IO], 
   expectedStatus: Status, 
   expectedBody: Option[String]
   ) = {
  val r: IO[(Status, String)] = for {
    client <- IO(clientFixture)
    response <- runReq(request, TestService(client.apply()).routes)
    body <- parseBody(response.body) 
  } yield (response.status, body)
  r.map(pair => {
    val (status,body) = pair
    assertEquals(status, expectedStatus)
    expectedBody match {
      case None => ()
      case Some(expectedStr) => assertEquals(body, expectedStr)
    }
  })
}
 

 test("Routes") {
  checkRequest(Request[IO]().withUri(uri"/hi"), Status.Ok, Some("\"hello\""))
 }
}