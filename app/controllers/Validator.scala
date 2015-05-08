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


object Validator extends Controller {
  
  import Utils._

  def onlyData(data: String, dataFormat: String) = Action {
    val vr = ValidationResult.empty
    val vf = ValidationForm(DataInput(data),DataOptions.default,false,SchemaInput(),SchemaOptions.default)
    Ok(views.html.validate_data(vr,vf))
  }

  def dataSchema(data: String, dataFormat: String, schema: String, schemaFormat: String) = Action {
    val vr = ValidationResult.empty
    val vf = ValidationForm(DataInput(data),DataOptions.default,true,SchemaInput(schema),SchemaOptions.default)
    Ok(views.html.validate_dataSchema(vr,vf))
  }

  def dataSchemaNode(data: String, dataFormat: String, schema: String, schemaFormat: String, node: String) = Action {
    val vr = ValidationResult.empty
    val vf = ValidationForm(DataInput(data),DataOptions.default,true,SchemaInput(schema),SchemaOptions.defaultWithIri(node))
    Ok(views.html.validate_dataSchemaNode(vr,vf))
  }

  def validate_get_Future(
          str_data: String
        , syntaxData: Option[String]
        , showData: Boolean
        , opt_schema: Option[String]
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
             format = RDFUtils.getFormat(syntaxData)
           , showData = showData 
           )
       
       val opts_schema = SchemaOptions(
            "SHEXC"
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
        , syntaxData: Option[String]
        , showData: Boolean
        , opt_schema: Option[String]
        , opt_iri: Option[String]
        , cut: Int
        , withIncoming: Boolean
        , withAny: Boolean
        , showSchema: Boolean
        ) = Action.async {  
      	validate_get_Future(str_data,syntaxData, showData, opt_schema, opt_iri, cut, withIncoming, withAny, showSchema).map(vrf => {
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
