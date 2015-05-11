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


trait Validator { this: Controller =>

  import Multipart._

  def onlyData(data: String, dataFormat: String) = { 
    validate_get(data,Some(dataFormat),true,None,None,None,10,false,false,false)
  }

  def dataSchema(data: String, dataFormat: String, schema: String, schemaFormat: String) = {
    validate_get(data,Some(dataFormat),true,Some(schema),Some(schemaFormat), None,10,false,false,false)
  }

  def dataSchemaNode(data: String, dataFormat: String, schema: String, schemaFormat: String, node: String) = {
    validate_get(data,Some(dataFormat),true,Some(schema),Some(schemaFormat),Some(node),10,false,false,false)
  }

  def validate_get_Future(
          str_data: String
        , formatData: Option[String]
        , showData: Boolean
        , opt_schema: Option[String]
        , schemaFormat: Option[String]
        , opt_iri: Option[String]
        , cut: Int
        , withIncoming: Boolean
        , withAny: Boolean
        , showSchema: Boolean
		) : Future[Try[ValidationResult]]= {
       val withSchema = opt_schema.isDefined
       val iri = opt_iri.map(str => IRI(str))
       val str_schema = opt_schema.getOrElse("")
       val opts_data = DataOptions(
             format = RDFUtils.getFormat(formatData)
           , showData = showData 
           )
       
       val opts_schema = SchemaOptions(
            SchemaUtils.getSchemaFormat(schemaFormat)
          , cut = cut
          , withIncoming = withIncoming
          , withAny = withAny
          , opt_iri = iri
          , showSchema
          )
      RDFParse(str_data,opts_data.format) match { 
        case Success(data) => 
          scala.concurrent.Future(Success(ValidationResult.validate(data,str_data,opts_data,withSchema,str_schema,opts_schema)))
        case Failure(e) => 
          scala.concurrent.Future(Success(
                ValidationResult(Some(false), 
                    "Error parsing Data with syntax " + opts_data.format + ": " + e.getMessage,
                    Stream(), 
                    List(), 
                    str_data, 
                    opts_data, 
                    withSchema, 
                    str_schema, 
                    opts_schema, 
                    PrefixMap.empty)
                ))
	 }
  }
  
  
  // TODO: Simplify this ugly code...long list of arguments
  def validate_get(
          str_data: String
        , dataFormat: Option[String]
        , showData: Boolean
        , opt_schema: Option[String]
        , schemaFormat: Option[String]  
        , opt_iri: Option[String]
        , cut: Int
        , withIncoming: Boolean
        , withAny: Boolean
        , showSchema: Boolean
        ) = Action.async {  
      	validate_get_Future(str_data,dataFormat, showData, opt_schema, schemaFormat, opt_iri, cut, withIncoming, withAny, showSchema).map(vrf => {
      	      vrf match {
      	        case Success(vr) => {
      	          val vf = ValidationForm.fromResult(vr)
      	          Ok(views.html.index(vr,vf))
      	        }
      	        case Failure(e) => BadRequest(e.getMessage)
      	      }
        	})
  }

    
  def validate_post = Action.async { request => {
      
     val pair = for ( vf <- getValidationForm(request)
                    ; str_data <- vf.dataInput.getDataStr
                    ) yield (vf,str_data)
      
     scala.concurrent.Future {
        pair match {
         case Success((vf,str_data)) => { 
        	  val tryValidate =
        	     for ( data <- vf.dataInput.getData(vf.dataOptions.format)
        	         ; str_schema <- vf.schemaInput.getSchemaStr
        	         )  
                 yield {
        	       ValidationResult.validate(data,str_data,vf.dataOptions, vf.withSchema, str_schema, vf.schemaOptions)
        	     }
              val vr = recover(tryValidate,recoverValidationResult(str_data,vf))
              Ok(views.html.index(vr,vf))
             }
       case Failure(e) => BadRequest(e.getMessage) 
      }
     }
    } 
  }
    
  def recoverValidationResult(str_data: String, vf: ValidationForm)(e: Throwable): ValidationResult = {
    val schema_str: String = Try(vf.schemaInput.getSchemaStr.get).getOrElse("")
    ValidationResult(Some(false),
        e.getMessage(),Stream(), List(), 
        str_data, vf.dataOptions, 
        vf.withSchema, schema_str, vf.schemaOptions, 
        PrefixMap.empty) 
  }
  

}

object Validator extends Controller with Validator