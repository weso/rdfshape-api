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

class ConverterSchemaTest 
  extends PlaySpec 
  with Results 
  with OneAppPerSuite {
  
  class ConverterController extends Controller with SchemaConverter
  
  "Converter#schema" should {
    "convert well formed Shape" in {
      val converter = new ConverterController()
      val schemaStr = """|prefix : <http://example.org/> 
                   |:a { :b . } 
                   |""".stripMargin
      val result = SchemaConverter.convert_schema_get(
          schema = schemaStr,
          inputFormat="SHEXC", 
          outputFormat="TURTLE").apply(FakeRequest())
      status(result) mustEqual OK    
      }
  }

  "Converter_schema_post" should {
    "convert well formed Schema by input" in {
      val converter = new ConverterController()
      val schemaStr = """|prefix : <http://example.org/> 
                   |:a { :b . } 
                   |""".stripMargin
                   
      val form = 
        MultipartFormData(
            Map( "schema" -> List("#schema")
               , "schema_uri" -> List("")
               , "schema_file" -> List("file")
               , "schema_textarea" -> List(schemaStr)
               ),
           List(FilePart("file", "message", Some("Content-Type: multipart/form-data"), 
                        play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))), 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/converter/schema").withMultipartFormDataBody(form)
      val result = converter.convert_schema_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must include("@prefix : &lt;http://example.org/&gt;")
      bodyText must include("rdf:Description")
      }
    
 }
}