package controllers

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

object Validator extends Controller {
  
    def validate = Action { request => {
	     val result = 
	       for ( mf <- getMultipartForm(request)
	    	   ; input_rdf <- parseInputType(mf, "rdf")
	    	   ; rdf <- parseRDF(mf, input_rdf)
	    	   ; withSchema <- parseWithSchema(mf)
	    	   ; input_shex <- parseInputType(mf, "schema")
	    	   ; withIRI <- parseWithIRI(mf)
	    	   ; withIncoming <- parseWithIncoming(mf)
	    	   ; iri <- parseIRI(mf)
	    	   ) yield {

	      val vr : ValidationResult = 
	        if (withSchema) {
	         parseSchema(mf,input_shex) match {
	           case Success((schema,pm)) => {
	             if (withIRI) {
	            	 val rs = Schema.matchSchema(iri,rdf,schema,withIncoming)
	            	 ValidationResult("Schema valid",rs.run,pm)
	             } else {
	               // val rs = Schema.matchAll(rdf,schema,withIncoming)
	               // ValidationResult("Schema valid",rs.run,pm)
	               ValidationResult.withMessage("Not implemented match all. Select an IRI")
	             } 
	           }
	           case Failure(e) => ValidationResult.failure(e)
	         }
	        } else {
	           ValidationResult.withMessage("RDF Parsed")
	        }

          val str_uri_rdf = parseKey(mf,"rdf_uri").getOrElse("") 
	      val str_rdf = parseKey(mf,"rdf_textarea").getOrElse("")
	      val str_schema = parseKey(mf,"schema_textarea").getOrElse("")
          val str_uri_schema = parseKey(mf,"schema_uri").getOrElse("") 
	      val str_iri = parseKey(mf,"iri").getOrElse("")

	      views.html.index(vr,
   		 		  	       input_rdf,withSchema,input_shex,withIRI,
   		 		  	       str_uri_rdf,str_rdf,str_uri_schema,str_schema,str_iri)
	     } 
        result match {
	       case Success(r) => Ok(r)
	       case Failure(e) => BadRequest("Failure: " + e.getMessage())
	     }
	 }
  }
  
  
  def getMultipartForm(request: Request[AnyContent]): Try[MultipartFormData[TemporaryFile]] = {
    val body: AnyContent = request.body
	 body.asMultipartFormData match {
      case Some(mf) => Success(mf)
      case None => Failure(new Exception("Expecting MultiformData request body"))
    }
  }

  def parseWithSchema(mf: MultipartFormData[TemporaryFile]): Try[Boolean] = {
    for (value <- parseKey(mf,"withSchema")) yield {
      value match {
        case "schemaYes" => true
        case "schemaNo" => false
        case _ => throw new Exception("parseWithSchema: unknown value " + value)
      }
    }
  }

  def parseWithIRI(mf: MultipartFormData[TemporaryFile]): Try[Boolean] = {
    for (value <- parseKey(mf,"withIRI")) yield {
      value match {
        case "true" => true
        case "false" => false
        case _ => throw new Exception("parseWithIRI: unknown value " + value)
      }
    }
  }

  def parseWithIncoming(mf: MultipartFormData[TemporaryFile]): Try[Boolean] = {
    for (value <- parseKey(mf,"withIncoming")) yield {
      value match {
        case "true" => true
        case "false" => false
        case _ => throw new Exception("parseWithIncoming: unknown value " + value)
      }
    }
  }

  def parseIRI(mf: MultipartFormData[TemporaryFile]): Try[IRI] = {
    for (value <- parseKey(mf,"iri")) yield IRI(value)
  }

  def parseInputType(mf: MultipartFormData[TemporaryFile], key: String): Try[InputType] = {
    for (value <- parseKey(mf,key))
      yield {
      value match {
    		case "byUri" => ByUri
    		case "byFile" => ByFile
    		case "byInput" => ByInput
    		case "byEndpoint" => ByEndpoint
    		case "byDereference" => ByDereference
    		case _ => throw new Exception("Unknown value for " + key + ": " + value)
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

 def parseRDF(mf: MultipartFormData[TemporaryFile],inputType: InputType): Try[RDF] = {
   inputType match {
     case ByUri => {
       for ( uri <- parseKey(mf,"rdf_uri")
           ; model <- JenaUtils.parseFromURI(uri)
           ) yield RDFFromJenaModel(model)
     }
     case ByFile => {
       mf.file("rdf_file") match {
         case Some(f) => {
           try {
            val filename 		= f.filename
            val contentType 	= f.contentType
            println("Contenttype: " + contentType)
            val input 			= new ByteArrayInputStream(FileUtils.readFileToByteArray(f.ref.file))
            JenaUtils.parseInputStream(input,"") match {
              case Parsed(model) => {
                   Success(RDFFromJenaModel(model))
                  }
              case NotParsed(err) => {
                   Failure(throw new Exception("Error parsing file: " + err))
              }
           }
          } catch {
            case e: Exception => Failure(e)
          }
         }
         case None => {
           Failure(throw new Exception("Input RDF by file but no file found for key rdf_file"))
       }
      }
     }
     case ByInput => {
       for ( cs <- parseKey(mf,"rdf_textarea")
           ; ts <- RDFTriples.parse(cs)
           ) yield ts
     } 
     case ByEndpoint => {
       for ( endpoint <- parseKey(mf,"rdf_endpoint")
           )
       yield Endpoint(endpoint)
     }
     case ByDereference => {
       Success(RDFFromWeb())
     }
   }
 }
 
 def notImplementedYet[A] : Try[A] = 
   Failure(throw new Exception("Not implemented yet"))

 def parseSchema(mf: MultipartFormData[TemporaryFile],inputType: InputType): Try[(Schema,PrefixMap)] = {
   inputType match {
     case ByUri => {
       notImplementedYet
     }
     case ByFile => {
       mf.file("schema_file") match {
         case Some(f) => {
           try {
            val filename 		= f.filename
            val contentType 	= f.contentType
            println("Contenttype: " + contentType)
            val input 			= new ByteArrayInputStream(FileUtils.readFileToByteArray(f.ref.file))
            notImplementedYet
           } catch {
            case e: Exception => Failure(e)
          }
         }
         case None => {
           Failure(throw new Exception("Input RDF by file but no file found for key rdf_file"))
       }
      }
     }
     case ByInput => {
       for ( cs <- parseKey(mf,"schema_textarea")
           ; (schema,pm) <- Schema.fromString(cs)
           ) yield (schema,pm)
     } 
     case _ => throw new Exception("parseSchema: non supported input type: " + inputType)
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
