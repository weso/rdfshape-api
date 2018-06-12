package es.weso.server

import java.io.{ByteArrayOutputStream, File}

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

  describe(s"Save to PNG") {
    import guru.nidi.graphviz.engine.Format
    import guru.nidi.graphviz.engine.Graphviz
    import guru.nidi.graphviz.model.Factory._
    val g = graph("example1").directed.`with`(node("a").link(node("b")))
    // Graphviz.fromGraph(g).width(200).render(Format.PNG).toFile(new File("f.png"))
    val image = Graphviz.fromGraph(g).width(200).render(Format.PNG).toImage
    println(s"Imagen: $image")
    import javax.imageio.ImageIO
    import javax.xml.bind.DatatypeConverter
    val baos = new ByteArrayOutputStream()
    ImageIO.write(image, "png", baos)
    val data = DatatypeConverter.printBase64Binary(baos.toByteArray)
    val imageString = "data:image/png;base64," + data
    println("<html><body><img src='" + imageString + "'></body></html>")
  }
}