package controllers 

import org.scalatest._
import org.scalatest.matchers._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import scala.collection.mutable.Stack
import play.api.mvc._
import scala.concurrent.Future
import views.html.helper._
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.{ Files => PlayFiles }
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

/*class DataConverterTest 
  extends PlaySpec 
  with Results 
  with OneAppPerSuite { */

object DataConverterTest extends PlaySpecification with Results {
  
  class ConverterController() extends Controller with DataConverter
  
  "Converter#data" should {
    "validate well formed RDF" in {
      val converter = new ConverterController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b :c . 
                   |""".stripMargin
      val result = converter.convert_data_get(
          data = rdfStr,
          dataFormat="TURTLE", 
          outputFormat="N-TRIPLES").apply(FakeRequest())
      status(result) mustEqual OK    
      }
  }

  "Converter_data_post" should {
    "convert well formed RDF by input" in {
      val converter = new ConverterController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b :c . 
                   |""".stripMargin
                   
      val form = 
        MultipartFormData(
            Map( "data" -> List("#byInput_data")
               , "data_uri" -> List("")
               , "data_file" -> List("file")
               , "data_textarea" -> List(rdfStr)
               , "data_format" -> List("TURTLE")
               , "showData" -> List("true")
               , "data_format" -> List("TURTLE")
               , "outputFormat" -> List("RDF/XML")
               , "schema" -> List("#no_schema")
               ),
           List(FilePart("file", "message", Some("Content-Type: multipart/form-data"), 
                        play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))), 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/converter/data").withMultipartFormDataBody(form)
      val result = converter.convert_data_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must be equalTo "ok" // include("@prefix : &lt;http://example.org/&gt;")
      //bodyText must include("rdf:Description")
      }
    
 }
}