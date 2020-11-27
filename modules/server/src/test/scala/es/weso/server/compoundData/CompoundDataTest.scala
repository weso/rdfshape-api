package es.weso.server.compoundData

import cats.implicits._
import es.weso.server.format._
import es.weso.server.merged.CompoundData._
import es.weso.server.merged.{CompoundData, DataElement, DataTextArea}
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should._

class CompoundDataTest extends AnyFunSpec with Matchers {
  describe(s"Compound data") {
    shouldParse(
      """|[{"data": "p", 
                     |  "dataFormat": "Turtle",
                     |  "activeDataTab": "#dataTextArea" 
                     | }
                     |]""".stripMargin,
      CompoundData(List(DataElement.empty.copy(data = Some("p"), dataFormat = Turtle, activeDataTab = DataTextArea)))
    ) 
    shouldParse(
      """|[{"data": "p", 
         |  "dataFormat": "json-ld",
         |  "activeDataTab": "#dataTextArea" 
         | }
         |]""".stripMargin,
      CompoundData(List(DataElement.empty.copy(data = Some("p"), dataFormat = JsonLd, activeDataTab = DataTextArea)))
    )
    shouldParse(
      """|[{"data": "p", 
         |  "dataFormat": "json-ld"
         | }
         |]""".stripMargin,
      CompoundData(List(DataElement.empty.copy(data = Some("p"), dataFormat = JsonLd, activeDataTab = DataTextArea)))
    )

    // Wrong value in "activeDataTab" should default to "#dataTextArea"
    shouldParse("""|[{"data": "p",
                      |  "dataFormat": "Json",
                      |  "activeDataTab": "#asdf"
                      | }
                      |]""".stripMargin,
      CompoundData(List(DataElement(Some("p"), None, None, None, dataFormat = JsonDataFormat, DataTextArea)))
    )

    shouldNotParse("""|[{"dataBadParam": "p",
                     |  "dataFormat": "Json",
                     |  "activeDataTab": "#asdf" 
                     | }
                     |]""".stripMargin) 
  }

  def shouldParse(str: String, expected: CompoundData): Unit = {
    it(s"Should parse\n$str\n and obtain \n${expected.asJson.spaces2}") {
      val r: Either[String, CompoundData] = for {
        json <- parse(str).leftMap(pe => s"Parser error: ${pe.getMessage()}")
        cd   <- json.as[CompoundData].leftMap(e => s"Error: ${e.getMessage()}")
      } yield cd

      r.fold(e => fail(s"Error: $e"), cd => if (cd == expected) 
        info(s"Equal to expected") else 
        fail(s"Parsed \n${cd.asJson.spaces2}\n is different to \n${expected.asJson.spaces2}\n"))
    }
  }

  def shouldNotParse(str: String): Unit = {
    it(s"Should not parse $str") {
      val r: Either[String, CompoundData] = for {
        json <- parse(str).leftMap(pe => s"Parser error: ${pe.getMessage()}")
        cd   <- json.as[CompoundData].leftMap(e => s"Error: ${e.getMessage()}")
      } yield cd

      r.fold(e => info(s"Error as expected: $e"), cd => fail(s"Parsed as $cd but should have failed"))
    }
  }

}
