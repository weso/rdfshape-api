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

class ValidatorTest 
  extends PlaySpec 
  with Results 
  with OneAppPerSuite {
  
  class ValidatorController extends Controller with Validator
  
  "Validator#onlyData" should {
    "validate well formed RDF" in {
      val validator = new ValidatorController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b :c . 
                   |""".stripMargin
      val result = validator.data(data = rdfStr, dataFormat="TURTLE", schemaVersion="SHACL").apply(FakeRequest())
      status(result) mustEqual OK    
      val bodyText : String = contentAsString(result)
      bodyText must include("@prefix : &lt;http://example.org/&gt;")
      }
  }

  "Validator_post" should {
    "validate well formed RDF by input" in {
      val validator = new ValidatorController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b :c . 
                   |""".stripMargin
                   
      val form = 
        MultipartFormData(
            Map( "data" -> List("#byInput_data")
               , "data_uri" -> List("")
               , "data_file" -> List("file")
               , "data_textarea" -> List(rdfStr)
               , "showData" -> List("true")
               , "data_format" -> List("TURTLE")
               , "schema" -> List("#no_schema")
               ),
           List(FilePart("file", "message", Some("Content-Type: multipart/form-data"), 
                        play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))), 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/validator").withMultipartFormDataBody(form)
      val result = validator.validate_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must include("@prefix : &lt;http://example.org/&gt;")
      bodyText must include("RDF parsed")
      }
    
    "validate bad formed RDF" in {
      val validator = new ValidatorController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b 
                   |""".stripMargin

      val files : List[MultipartFormData.FilePart[PlayFiles.TemporaryFile]] = List() 
/*        List(FilePart("file", 
                      "message", 
                      Some("Content-Type: multipart/form-data"), 
                        play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))) */             
      val form = 
        MultipartFormData(
            Map( "data" -> List("#byInput_data")
               , "data_uri" -> List("")
               , "data_file" -> List("file")
               , "data_textarea" -> List(rdfStr)
               , "showData" -> List("true")
               , "data_format" -> List("TURTLE")
               , "schema" -> List("#no_schema")
               ),
           files, 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/validator").withMultipartFormDataBody(form)
      val result = validator.validate_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must include("result_bad")
      }

    "validate well formed RDF by File" in {
      val validator = new ValidatorController()
      val rdfStr = """|@prefix : <http://example.org/> .
                   |:a :b :c . 
                   |""".stripMargin
                
    val fileName = "tempFile.txt"

    try {               
    Files.write(Paths.get(fileName), rdfStr.getBytes(StandardCharsets.UTF_8))
    
    val files = List(FilePart("data_file", 
                         rdfStr, 
                         Some("Content-Type: multipart/form-data"), 
                         PlayFiles.TemporaryFile(new java.io.File(fileName))))             
    val form = 
        MultipartFormData(
            Map( "data" -> List("#byFile_data")
               , "data_uri" -> List("")
               , "data_file" -> List("data_file")
               , "data_textarea" -> List("")
               , "showData" -> List("true")
               , "data_format" -> List("TURTLE")
               , "schema" -> List("#no_schema")
               ),
           files, 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/validator").withMultipartFormDataBody(form)
      val result = validator.validate_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must include("@prefix : &lt;http://example.org/&gt;")
      bodyText must include("RDF parsed")
    } finally {
      // Files.delete(Paths.get(fileName))
    }
    }
    
  }
}