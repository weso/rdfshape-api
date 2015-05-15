package controllers

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.ByteArrayInputStream
import org.apache.commons.io.FileUtils
import play.api._
import play.api.mvc._
import play.api.libs.Files._
import es.weso.shex.Schema
import scala.util._
import es.weso.rdf._
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.jena._
import es.weso.monads.{Result => SchemaResult, Failure => SchemaFailure, Passed}
import es.weso.shex.Typing
import es.weso.utils._
import es.weso.utils.TryUtils._
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import java.net.URL
import java.io.File


trait Checker { this: Controller =>

  import Multipart._

  def data(data: String, dataFormat: String) = { 
    Validator.validate_get(data,Some(dataFormat),true,None,None,"",None,10,false,false,false)
  }
  
  def schema(schema: String, schemaFormat: String, schemaVersion: String) = 
    Action.async {  
     schema_Future(schema,schemaFormat, schemaVersion).map(result => {
               result match {
                case Success(str) => {
                  val schemaInput = SchemaInput(schema,schemaFormat,schemaVersion)
                  val vf = ValidationForm.fromSchemaConversion(schemaInput)
                  Ok(views.html.check_schema(vf,str))
                }
                case Failure(e) => BadRequest(e.getMessage)
              }
          })
    }

  
  def schema_Future(
          schema: String
        , schemaFormat: String
        , schemaVersion: String
        ) : Future[Try[String]]= {
    val schemaInput = SchemaInput(schema,schemaFormat,schemaVersion)
    Future(schemaInput.convertSchema(schemaFormat))
  }   
  
  
  def schema_post = Action { request => {
     val r = for ( mf <- getMultipartForm(request)
                 ; schemaInput <- parseSchemaInput(mf)
                 ; str_schema <- schemaInput.getSchemaStr
                 ; outputStr <- schemaInput.convertSchema(schemaInput.inputFormat)
                 ) yield (schemaInput, outputStr)
     
      r match {
       case Success((schemaInput, result)) => {
         val vf = ValidationForm.fromSchemaConversion(schemaInput)
         Ok(views.html.check_schema(vf,result))
       }
       case Failure(e) => BadRequest(e.getMessage) 
      }
    } 
  }
 

}

object Checker extends Controller with Checker