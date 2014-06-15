package controllers

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.concurrent.promise
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.ByteArrayInputStream
import org.apache.commons.io.FileUtils
import play.api._
import play.api.mvc._
import play.api.libs.Files._
import es.weso.shex.Schema
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import es.weso.rdf.RDFTriples
import es.weso.rdf.RDF
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.reader.Endpoint
import es.weso.rdf.reader.RDFFromWeb
import es.weso.monads.{Result => SchemaResult, Failure => SchemaFailure}
import es.weso.shex.Typing
import es.weso.monads.Passed
import es.weso.rdf.reader.RDFFromJenaModel
import es.weso.utils._
import es.weso.parser.PrefixMap
import java.net.URL
import java.io.File
import views.html.defaultpages.badRequest
import es.weso.utils.IOUtils._

object Validator extends Controller {

  def validate_get_Future(
          str_rdf: String
        , opt_schema: Option[String]
        , opt_iri: Option[String]
        , cut: Int
        , withIncoming: Boolean
        , openClosed: Boolean
        , withAny: Boolean
		) : Future[Try[ValidationResult]]= {
       val withSchema = opt_schema.isDefined
       val iri = opt_iri.map(str => IRI(str))
       val str_schema = opt_schema.getOrElse("")
       val opts_schema = SchemaOptions(
            cut = cut
          , withIncoming = withIncoming
          , openClosed = openClosed
          , withAny = withAny
          , opt_iri = iri
          )
      RDFParse(str_rdf) match { 
        case Success((rdf,_)) => 
          scala.concurrent.Future(Success(ValidationResult.validate(rdf,str_rdf,withSchema,str_schema,opts_schema)))
        case Failure(e) => 
          scala.concurrent.Future(Failure(e))
	 }
  }
  
  def validate_get(
          str_rdf: String
        , opt_schema: Option[String]
        , opt_iri: Option[String]
        , cut: Int
        , withIncoming: Boolean
        , openClosed: Boolean
        , withAny: Boolean
        ) = Action.async {  
      	validate_get_Future(str_rdf,opt_schema, opt_iri, cut, withIncoming,openClosed,withAny).map(vrf => {
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
      val resf : Future[Try[(ValidationResult,ValidationForm)]] = 
        scala.concurrent.Future { 
         for ( vf <- getValidationForm(request)
             ; (rdf,str_rdf) <- getRDF(vf)
             ; opt_schema <- vf.getSchemaStr
             ; schemaOptions <- vf.getSchemaOptions
             ) 
         yield (ValidationResult.validate(rdf,str_rdf,opt_schema.isDefined, opt_schema.getOrElse(""), schemaOptions.getOrElse(SchemaOptions.default)),vf)
      }
            
      resf.map(res =>
        res match {
        	case Success((vr,vf)) => Ok(views.html.index(vr,vf))
        	case Failure(e) => BadRequest(e.getMessage)
        }
     )
    }
  }
    
  def getValidationForm(request: Request[AnyContent]): Try[ValidationForm] = {
    for ( mf <- getMultipartForm(request)
        ; input_type_rdf <- parseInputType(mf,"rdf")
        ; rdf_uri <- parseKey(mf,"rdf_uri")
        ; rdf_textarea <- parseKey(mf,"rdf_textarea")
        ; rdf_file <- parseFile(mf,"rdf_file")
        ; rdf_endpoint <- parseKey(mf,"rdf_endpoint")
        ; opt_schema <- parseOptSchema(mf)
        )
    yield {
     val has_schema = opt_schema.isDefined
     val input_schema = if (has_schema) 
    					opt_schema.get._1 
    				 else 
    				    SchemaInput()
    				    
     val opts_schema = if (has_schema) 
    					opt_schema.get._2 
    				  else 
    				    SchemaOptions.default
      ValidationForm(
        input_type_rdf,
        rdf_uri, rdf_file, rdf_textarea, rdf_endpoint,
        has_schema, input_schema, opts_schema
        )
    }
  }
  
  def getMultipartForm(request: Request[AnyContent]): Try[MultipartFormData[TemporaryFile]] = {
    val body: AnyContent = request.body
	 body.asMultipartFormData match {
      case Some(mf) => Success(mf)
      case None => Failure(new Exception("Expecting MultiformData request body"))
    }
  }

  
  def parseFile(mf: MultipartFormData[TemporaryFile], key:String) : Try[Option[File]] = {
    mf.file(key) match {
         case Some(f) => Success(Some(f.ref.file))
         case None => Success(None)
    }
  }
  
  def parseOptIRI(mf: MultipartFormData[TemporaryFile]): Try[Option[IRI]] = {
    val withIRI = parseWithIRI(mf)
    withIRI match {
      case Success(true) => {
        parseIRI(mf) match {
            case Success(iri) => Success(Some(iri))
            case Failure(e) => Failure(e)
          }
      }
      case Success(false) => Success(None)
      case Failure(e) => Failure(e)  
    }
  }

  def parseWithIRI(mf: MultipartFormData[TemporaryFile]): Try[Boolean] = {
    for (value <- parseKey(mf,"withIRI")) yield {
      if (value.startsWith("#noIri")) false
      else if (value.startsWith("#iri")) true
      else throw new Exception("parseWithIRI: unknown value " + value)
    }
  }

  def parseIRI(mf: MultipartFormData[TemporaryFile]): Try[IRI] = {
    for (value <- parseKey(mf,"iri")) yield IRI(value)
  }

  def parseOptSchema(mf: MultipartFormData[TemporaryFile]): Try[Option[(SchemaInput,SchemaOptions)]] = {
    for (value <- parseKey(mf,"schema")) yield {
      value match {
        case "#schema" => {
          val opts = 
            for ( input <- parseSchemaInput(mf)
                ; options <- parseSchemaOptions(mf)
                ) yield (input,options)
            opts.map(pair => Some(pair)).get
        }
        case "#no_schema" => 
          None
        case _ => 
          throw new Exception("Unknown value for key schema: " + value)
      }
    }
  }

  def parseSchemaInput(mf: MultipartFormData[TemporaryFile]): Try[SchemaInput] = {
    for ( input_type_schema <- parseInputType(mf,"input-schema")
        ; schema_uri <- parseKey(mf,"schema_uri")
        ; schema_file <- parseFile(mf,"schema_file")
        ; schema_textarea <- parseKey(mf,"schema_textarea")
        )
   yield
     SchemaInput(input_type_schema,schema_uri, schema_file, schema_textarea)
  }

  def parseSchemaOptions(mf: MultipartFormData[TemporaryFile]): Try[SchemaOptions] = {
    for ( cut <- parseInt(mf,"cut",0,100)
        ; withIncoming <- parseBoolean(mf,"withIncoming")
        ; openClosed <- parseBoolean(mf,"openClosed")
        ; withAny <- parseBoolean(mf,"withAny")
        ; opt_iri <- parseOptIRI(mf)
        )
   yield
     SchemaOptions(cut,withIncoming,openClosed,withAny,opt_iri)
  }

  def parseBoolean(mf: MultipartFormData[TemporaryFile], key: String): Try[Boolean] = {
    for (value <- parseKey(mf,key)) yield {
      value match {
        case "true" => true
        case "false" => false
        case _ => throw new Exception("parseBoolean: unknown value " + value + " for key " + key)
      }
    }
  }

  def parseInt(mf: MultipartFormData[TemporaryFile], key: String, min: Int, max:Int): Try[Int] = {
    for (value <- parseKey(mf,key)) yield {
      val n = value.toInt
      if (n < min || n > max) throw new Exception("parseInt, n " + n + " must be between " + min + " and " + max)
      else n
    }
  }

  def parseInputType(mf: MultipartFormData[TemporaryFile], key: String): Try[InputType] = {
    for (value <- parseKey(mf,key))
      yield {
      // The following code is to transform #XXX_* to XXX 
      val pattern = ("#(.*)_.*" ).r
      val extracted = pattern.findFirstMatchIn(value).map(_ group 1)
      extracted match {
    		case Some("byUri") => ByUri
    		case Some("byFile") => ByFile
    		case Some("byInput") => ByInput
    		case Some("byEndpoint")  => ByEndpoint
    		case Some("byDereference") => ByDereference
    		case x => throw new Exception("Unknown value for " + key + ": " + value + ". match = " + x)
    }
   }
 } 
  
 def parseKey(mf: MultipartFormData[TemporaryFile],key:String): Try[String] = {
   if (mf.asFormUrlEncoded(key).size == 1) {
      Success(mf.asFormUrlEncoded(key).head)
   } else Failure(throw new 
        Exception("parseKey: key " + key + 
    		" must have one value but it has = " + mf.asFormUrlEncoded(key)))
 }

 def RDFParse(str: String): Try[(RDF,String)] = {
   RDFTriples.parse(str) match {
     case Success(rdf) => Success((rdf,str))
     case Failure(e) => 
       Failure(throw new Exception("Exception :" + e.getMessage + "\nParsing RDF:\n" + str))
   }
 }
 
 def getRDF(vf: ValidationForm): Try[(RDF,String)] = {
     vf.input_type_RDF match {
     case ByUri => for ( str <- getURI(vf.rdf_uri)
                       ; pair <- RDFParse(str)
                       ) yield pair
     case ByFile => for ( str <- getFileContents(vf.rdf_file)
    		 		    ; pair <- RDFParse(str)
    		 		    ) yield pair
     case ByInput => {
       val str = vf.rdf_textarea
       RDFParse(str)
     } 
     case ByEndpoint => {
       Success(Endpoint(vf.rdf_endpoint),"")
     }
     case ByDereference => {
       Success((RDFFromWeb(),""))
     }
     case _ => Failure(throw new Exception("getRDF: Unknown input type"))
   }
 }
 
  // TODO: Move this method to a es.weso.monads.Result
  def liftTry[A](t:Try[A]):SchemaResult[A] = {
    t match {
      case Failure(e) => SchemaFailure(e.getMessage())
      case Success(v) => Passed(Stream(v))
    }
  }

}

