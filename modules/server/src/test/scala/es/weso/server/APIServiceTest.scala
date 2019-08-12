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
import org.scalatest._
import es.weso.utils.test._

import scala.concurrent.ExecutionContext.global

class APIServiceTest extends FunSpec with Matchers with EitherValues with JsonMatchers {

  describe(s"API Service") {
    it("Should obtain JSON data from RDF") {
      val dataStr =
        """prefix : <http://example.org/>
          |:x :p 1 .
          |""".stripMargin

      val ioResponse = serve(Request(
        GET,
        Uri(
          path = "/api/data/convert",
          query = HQuery.fromPairs(
            (QueryParams.data, dataStr),
            (QueryParams.dataFormat, "Turtle"),
            (QueryParams.targetDataFormat, "JSON")
          ))))

      val response = ioResponse.unsafeRunSync
      response.status should be(Ok)
      val strResponse = response.as[String].unsafeRunSync()
      val jsonResponse = parse(strResponse).getOrElse(Json.Null)
      val expected =
        """|[
           | { "data": { "id": "N0", "type": "iri", "label": ":x" } },
           | { "data": { "id": "N1", "type": "lit", "label": "1" } },
           | { "data": { "source": "N0", "target": "N1", "label": ":p", "href": "http://example.org/p"} }
           |]""".stripMargin
      jsonResponse should matchJsonString(expected)
    }
  }

  val ip = "0.0.0.0"
  val port = 8080
  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def serve(req: Request[IO]): IO[Response[IO]] = {
    val blocker = Blocker[IO]

    blocker.use { case (blocker) =>
      BlazeClientBuilder[IO](global).resource.use { case client =>
       APIService[IO](blocker, client).routes.orNotFound.run(req)
     }
    }
  }
}
