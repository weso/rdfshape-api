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
import scala.util._
import es.weso.rdf._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.jena._
import es.weso.monads.{Result => SchemaResult, Failure => SchemaFailure, Passed}
import es.weso.utils._
import es.weso.utils.TryUtils._
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import java.net.URL
import java.io.File
import scala.util.{Success => TrySuccess}
import DataOptions._
import SchemaOptions._


trait Checker { this: Controller =>

  import Multipart._

  def data(data: String, dataFormat: String) = { 
    ValidatorSchemaData.validate_get(data,
        Some(dataFormat),
        DEFAULT_SHOW_DATA,
        None,
        None,
        "",
        None,
        DEFAULT_CUT,
        DEFAULT_ShowSchema)
  }
  
  def schema(schema: String, schemaFormat: String, schemaName: String) = {
    println("Schema name: " + schemaName)
    Action.async {  
     schema_Future(schema,schemaFormat, schemaName).map(result => {
               result match {
                case TrySuccess(str) => {
                  val schemaInput = SchemaInput(schema,schemaFormat,schemaName)
                  val vf = ValidationForm.fromSchemaConversion(schemaInput)
                  Ok(views.html.check_schema(vf,str))
                }
                case Failure(e) => 
                  BadRequest(views.html.errorPage(e.getMessage))
              }
          })
    }
  }

  
  def schema_Future(
          schema: String
        , schemaFormat: String
        , schemaVersion: String
        ) : Future[Try[String]]= {
    val schemaInput = SchemaInput(schema,schemaFormat,schemaVersion)
    val output = schemaInput.convertSchema(schemaFormat)
    Future(output)
  }   
  
  
  def schema_post = Action { request => {
     val r = for ( mf <- getMultipartForm(request)
                 ; schemaInput <- parseSchemaInput(mf)
                 ; str_schema <- schemaInput.getSchemaStr
                 ; outputStr <- schemaInput.convertSchema(schemaInput.inputFormat)
                 ) yield (schemaInput, outputStr)
     
      r match {
       case TrySuccess((schemaInput, result)) => {
         val vf = ValidationForm.fromSchemaConversion(schemaInput)
         Ok(views.html.check_schema(vf,result))
       }
       case Failure(e) => BadRequest(views.html.errorPage(e.getMessage)) 
      }
    } 
  }
 

}

object Checker extends Controller with Checker