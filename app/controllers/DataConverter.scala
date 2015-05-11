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
import Multipart._
import play.api.libs.json._

trait DataConverter { this: Controller => 

 def converterDataFuture(
          data: String
        , dataFormat: String
        , outputFormat: String
    ) : Future[Try[String]]= {
       Future(RDFParse(data,dataFormat).map(_.serialize(outputFormat)))
  }
  
  
  def convert_data_get(
          data: String
        , dataFormat: String
        , outputFormat: String
        ) = Action.async {  
        converterDataFuture(data,dataFormat, outputFormat).map(output => {
              output match {
                case Success(result) => {
                  val vf = ValidationForm.fromDataConversion(data,dataFormat)
                  Ok(views.html.convert_data(vf,outputFormat,result))
                }
                case Failure(e) => BadRequest(e.getMessage)
              }
          })
  }


    def convert_data_post = Action { request => {
     val r = for ( 
       mf <- getMultipartForm(request)
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
  
  def dataFormats = Action {
    Ok(Json.toJson(DataFormats.toList))
  }
   
}

object DataConverter extends Controller with DataConverter
