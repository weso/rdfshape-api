package es.weso.server

import cats.effect.IO
import es.weso.rdf.jena.RDFAsJenaModel
import guru.nidi.graphviz.engine.Format
import org.http4s.server.Server
import org.scalatest._
import org.scalatest.selenium._


class DotConverterTest extends FunSpec
  with Matchers  {
 describe(s"Dot converter") {
   it(s"Should convert to PNG") {
     val rdfStr =
       """|<x> <p> 1 .
          |
       """.stripMargin

     val eitherResult = for {
       rdf <- RDFAsJenaModel.fromChars(rdfStr, "TURTLE")
       dot <- rdf.serialize("DOT")
       cnv <- APIService.dotConverter(dot, Format.PNG)
     } yield cnv
     eitherResult.fold(
       e => fail("Error: $e"),
       r => info(s"Result: $r")
     )
   }
 }
}