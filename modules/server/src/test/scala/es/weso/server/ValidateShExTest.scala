package es.weso.server

import cats.effect.IO
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.shapeMaps.{Status => ShapeMapStatus, _}
import io.circe.Json
import io.circe.parser._
import org.http4s.{Request, Response, Uri}
import org.http4s.{Query => HQuery}
import org.scalatest._
import org.http4s.dsl.io._

class ValidateShExTest extends FunSpec with Matchers with EitherValues {

  val ip = "0.0.0.0"
  val port = 8080
  val server = new RDFShapeServer(ip, port)

  def serve(req: Request[IO]): Response[IO] =
    server.service.orNotFound(req).unsafeRunSync

  describe("ValidateShEx") {
/*    it("Should return 200 when asking for root") {
      val response = serve(Request(GET, Uri(path = "/")))
      response.status should be(Ok)
    }

    it("Should run test API method") {
      val response = serve(Request(
        GET,
        Uri(
          path = "/api/test",
          query = HQuery.fromPairs(("name", "<John>")))))
      response.status should be(Ok)
      response.as[String].unsafeRunSync() should be("Hello <John>")
    }

    it("Should validate a single example using ShEx and TargetDecls") {
      val dataStr =
        """prefix : <http://example.org/>
          |prefix sh: <http://www.w3.org/ns/shacl#>
          |:x :p 1 .
          |:S sh:targetNode :x .
          |""".stripMargin

      val schemaStr =
        """prefix : <http://example.org/>
          |:S { :p . }
          |""".stripMargin

      val response = serve(Request(
        GET,
        Uri(
          path = "/api/validate",
          query = HQuery.fromPairs(
            ("data", dataStr),
            ("schema", schemaStr),
            ("schemaFormat", "SHEXC"),
            ("triggerMode", "TargetDecls"),
            ("schemaEngine", "ShEx")))))

      response.status should be(Ok)
      val strResponse = response.as[String].unsafeRunSync()
      val jsonResponse = parse(strResponse).getOrElse(Json.Null)
      val isValid: Option[Boolean] =
        jsonResponse.hcursor.get[Boolean]("valid").toOption
      isValid shouldBe Some(true)
    }
*/
    it("Should validate a single example using ShEx and shapeMap") {
      val dataStr =
        """prefix : <http://example.org/>
          |:x :p 1 .
          |""".stripMargin

      val schemaStr =
        """prefix : <http://example.org/>
          |:S { :p [ 1 2 ] }
          |""".stripMargin

      val shapeMapStr = ":x@:S"

      val response = serve(Request(
        GET,
        Uri(
          path = "/api/validate",
          query = HQuery.fromPairs(
            ("data", dataStr),
            ("schema", schemaStr),
            ("schemaFormat", "SHEXC"),
            ("triggerMode", "ShapeMap"),
            ("shape-map", shapeMapStr),
            ("schemaEngine", "ShEx")
          ))))

      response.status should be(Ok)
      val strResponse = response.as[String].unsafeRunSync()
      val jsonResponse = parse(strResponse).getOrElse(Json.Null)
      val isValid: Option[Boolean] =
        jsonResponse.hcursor.get[Boolean]("valid").toOption
      isValid shouldBe Some(true)
      val x = IRI("http://example.org/x")
      val s: ShapeMapLabel = IRILabel(IRI("http://example.org/S"))
      shapeMapStatus(jsonResponse,x,s,Conformant)
    }

    it("Should fail to validate a wrong example using ShEx and shapeMap") {
      val dataStr =
        """prefix : <http://example.org/>
          |:x :p 1 .
          |""".stripMargin

      val schemaStr =
        """prefix : <http://example.org/>
          |:S { :p [2 3] }
          |""".stripMargin

      val shapeMapStr = ":x@:S"

      val response = serve(Request(
        GET,
        Uri(
          path = "/api/validate",
          query = HQuery.fromPairs(
            ("data", dataStr),
            ("schema", schemaStr),
            ("schemaFormat", "SHEXC"),
            ("triggerMode", "ShapeMap"),
            ("shape-map", shapeMapStr),
            ("schemaEngine", "ShEx")
          ))))

      response.status should be(Ok)
      val strResponse = response.as[String].unsafeRunSync()
      val jsonResponse = parse(strResponse).getOrElse(Json.Null)
      val isValid: Option[Boolean] =
        jsonResponse.hcursor.get[Boolean]("valid").toOption
      isValid shouldBe Some(true)
      val x = IRI("http://example.org/x")
      val s: ShapeMapLabel = IRILabel(IRI("http://example.org/S"))
      shapeMapStatus(jsonResponse,x,s,NonConformant)
    }

    def isNodeShape(a: Association, x : RDFNode, s: ShapeMapLabel): Boolean = a.node match {
      case RDFNodeSelector(n) => n == x && a.shape == s
      case _ => false
    }

    def shapeMapStatus(response: Json, node: RDFNode, label: ShapeMapLabel, status: ShapeMapStatus): Unit = {
      response.hcursor.downField("shapeMap").as[ShapeMap].fold(
        failure => fail(failure.message),
        shapeMapReturned => {
          shapeMapReturned.associations.filter(isNodeShape(_,node,label)).headOption.fold(
            fail(s"No association found for node $node in shapeMap $shapeMapReturned")
          )(a =>
            a.info.status should be(status)
          )
        }
      )
    }

  }
}
