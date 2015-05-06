package controllers

import play.api._
import play.api.mvc._
import scala.concurrent._
import akka.actor._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.ByteArrayInputStream
import play.api._
import play.api.mvc._
import play.api.libs.Files._
import es.weso.shex.Schema
import scala.util._
import es.weso.rdf._
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.jena._
import es.weso.monads.{Result => SchemaResult, Failure => SchemaFailure}
import es.weso.shex._
import es.weso.utils._
import es.weso.utils.TryUtils._
import es.weso.utils.RDFUtils._
import java.net.URL
import java.io.File
import es.weso.utils.IOUtils._
import Utils._
import play.api.libs.json._

object Converter extends Controller {

  def data(data: String, inputFormat: String, outputFormat: String) = Action {
    val maybe = for { rdf <- RDFAsJenaModel.fromChars(data,inputFormat)
    } yield rdf.serialize(outputFormat)
    maybe match {
      case Success(str) => {
       val vf = ValidationForm.fromDataConversion(data,inputFormat)
       Ok(views.html.convert_data(vf,outputFormat,str)) 
      }
      case Failure(e) => Ok("Exception reading contents " + e.getMessage) 
    }
  }

    def convert_data_post = Action { request => {
      
     val r = for ( mf <- getMultipartForm(request)
                   ; vf <- getValidationForm(request)
                   ; str_data <- vf.dataInput.getDataStr
                   ; outputFormat <- parseKey(mf, "outputFormat")
                   ; data <- vf.dataInput.getData(vf.dataOptions.format)
                   ) yield (vf,outputFormat,data.serialize(outputFormat))
     
      r match {
       case Success((vf,outputFormat,result)) =>
             Ok(views.html.convert_data(vf,outputFormat,result))
       case Failure(e) => BadRequest(e.getMessage) 
      }
    } 
  }

  def schema(schema: String, inputFormat: String, outputFormat: String) = Action {
    val maybe = for { 
      (schema,_) <- Schema.fromString(schema,inputFormat)
    } yield schema.serialize(outputFormat)
    maybe match {
      case Success(str) => {
       val vf = ValidationForm.fromSchemaConversion(schema,inputFormat)
       Ok(views.html.convert_schema(vf,outputFormat,str)) 
      }
      case Failure(e) => Ok("Exception reading contents " + e.getMessage) 
    }
  }

  def convert_schema_post = Action { request => {
     val r = for ( mf <- getMultipartForm(request)
                 ; vf <- getValidationForm(request)
                 ; str_schema <- vf.schemaInput.getSchemaStr
                 ; outputFormat <- parseKey(mf, "outputFormat")
                 ; schema <- vf.schemaInput.getSchema(vf.dataOptions.format)
                 ) yield (vf,outputFormat,schema.serialize(outputFormat))
     
      r match {
       case Success((vf,outputFormat,result)) =>
             Ok(views.html.convert_schema(vf,outputFormat,result))
       case Failure(e) => BadRequest(e.getMessage) 
      }
    } 
  }

  
  def dataFormats = Action {
    Ok(Json.toJson(DataFormats.toList))
  }
   
  
  def schemaFormats = Action {
    Ok(Json.toJson(SchemaFormats.toList))
  }
    
}