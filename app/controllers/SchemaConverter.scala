package controllers

import scala.concurrent.Future
import scala.util.{ Failure => TryFailure, Success => TrySuccess, Try }

import Multipart.{ getMultipartForm, parseKey, parseSchemaInput }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import es.weso.schema._

trait SchemaConverter { this: Controller => 

 def converterSchemaFuture(
          schema: String
        , inputFormat: String
        , schemaVersion: String
        , outputFormat: String
    ) : Future[Try[String]]= {
       val schemaInput = SchemaInput(schema,inputFormat,schemaVersion)
       Future(schemaInput.convertSchema(outputFormat))
  }
  
  
  def convert_schema_get(
          schema: String
        , inputFormat: String
        , schemaVersion: String
        , outputFormat: String
        ) = Action.async {  
        converterSchemaFuture(schema,inputFormat, schemaVersion,outputFormat).map(output => {
              output match {
                case TrySuccess(result) => {
                  val schemaInput = SchemaInput(schema,inputFormat,schemaVersion)
                  val vf = ValidationForm.fromSchemaConversion(schemaInput)
                  Ok(views.html.convert_schema(vf,outputFormat,result))
                }
                case TryFailure(e) => BadRequest(views.html.errorPage(e.getMessage))
              }
          })
  }

  def convert_schema_post = Action { request => {
     val r = for ( mf <- getMultipartForm(request)
                 ; schemaInput <- parseSchemaInput(mf)
                 ; str_schema <- schemaInput.getSchemaStr
                 ; outputFormat <- parseKey(mf, "outputFormat")
                 ; outputStr <- schemaInput.convertSchema(outputFormat)
                 ) yield (schemaInput, outputFormat,outputStr)
     
      r match {
       case TrySuccess((schemaInput, outputFormat,result)) => {
         val vf = ValidationForm.fromSchemaConversion(schemaInput)
         Ok(views.html.convert_schema(vf,outputFormat,result))
       }
       case TryFailure(e) => {
        Logger.info("Exception raised: " + e.getMessage)
        BadRequest(views.html.errorPage(e.getMessage)) 
       } 
      }
    } 
  }

  def schemaFormats = Action {
    Ok(Json.toJson(Schemas.availableFormats))
  }
    
}

object SchemaConverter extends Controller with SchemaConverter
