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
import xml.Utility._

class SchemaSyntaxTest 
  extends PlaySpec 
  with Results 
  with OneAppPerSuite {
  
  class CheckerController extends Controller with Checker
  
  "Checker#schema" should {
    /*"convert simple well formed Shape in SHEXC" in {
      val checker = new CheckerController()
      val schemaStr = """|prefix : <http://example.org/> 
                   |:a { :b IRI } 
                   |""".stripMargin
      val result = checker.schema(
          schema = schemaStr,
          schemaFormat="SHEXC", 
          schemaVersion="SHACL_0.1").apply(FakeRequest())
      
      val bodyText : String = contentAsString(result)
      bodyText must include(escape(schemaStr))
      }*/
    
    "convert simple well formed Shape in TURTLE" in {
      val checker = new CheckerController()
      val schemaStr = """|@prefix :      <http://pepe.com/> .
                         |@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
                         |@prefix sh:    <http://www.w3.org/ns/shacl/core#> .
                         |
                         |:a      a            sh:Shape ;
                         |        sh:property  [ a             sh:PropertyConstraint ;
                         |                       sh:minCount   1 ;
                         |                       sh:nodeKind   sh:IRI ;
                         |                       sh:predicate  :b ] .
                         |""".stripMargin
      val result = checker.schema(
          schema = schemaStr,
          schemaFormat="TURTLE", 
          schemaVersion="SHACL_0.1").apply(FakeRequest())
      
      val bodyText : String = contentAsString(result)
      bodyText must include(escape(schemaStr))
      }

  }

/*  "Checker_schema_post" should {
    "convert well formed Schema by input POST" in {
      val checker = new CheckerController()
      val schemaStr = """|prefix : <http://example.org/> 
                   |:a { :b IRI } 
                   |""".stripMargin
                   
      val form = 
        MultipartFormData(
            Map( "schema" -> List("#schema")
               , "input-schema" -> List("#byInput_schema")
               , "schema_uri" -> List("")
               , "schema_file" -> List("file")
               , "schema_textarea" -> List(schemaStr)
               , "schema_format" -> List("SHEXC")
               , "schema_version" -> List("SHACL_0.1")
               ),
           List(FilePart("file", "message", Some("Content-Type: multipart/form-data"), 
                        play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))), 
           List(), 
           List())
      val request = FakeRequest(POST, "/api/converter/schema").withMultipartFormDataBody(form)
      val result = checker.schema_post().apply(request)
      val bodyText : String = contentAsString(result)
      bodyText must include(escape(schemaStr))
      }
    
 } */
}