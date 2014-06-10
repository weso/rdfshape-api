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
import java.net.URL
import java.io.File
import views.html.defaultpages.badRequest

object Validator extends Controller {
  
    def validate_get(
        str_rdf: String,
        opt_schema: Option[String],
        opt_iri: Option[String],
        withIncoming: Boolean
        ) = Action { request => { 

      val iri = opt_iri.map(str => IRI(str))
      RDFTriples.parse(str_rdf) match {
        case Success(rdf) => {
        	val vr = ValidationResult.validate(rdf,str_rdf,opt_schema,iri,withIncoming)
        	val vf = ValidationForm.fromResult(vr)
        	Ok(views.html.index(vr,vf))
        }
        case Failure(e) => {
          BadRequest("Cannot parse RDF\nError: " + e.getMessage + "\nRDF:\n " + str_rdf)
        }
      }
	}
  }      

    
    def validate_post = Action { request => {
      val res = 
        for ( vf <- getValidationForm(request)
            ; (rdf,str_rdf) <- getRDF(vf)
            ; opt_schema <- getSchemaStr(vf)
            ) 
        yield {
          val vr = ValidationResult.validate(rdf,str_rdf,opt_schema,vf.opt_iri,vf.withIncoming)
          (vr,vf)
        }
      res match {
        case Success((vr,vf)) => Ok(views.html.index(vr,vf))
        case Failure(e) => BadRequest(e.getMessage)
      }
     }
    }
    
  def getValidationForm(request: Request[AnyContent]): Try[ValidationForm] = {
    for ( mf <- getMultipartForm(request)
        ; input_type_rdf <- parseInputType(mf,"rdf")
        ; rdf_uri <- parseKey(mf,"rdf_uri")
        ; rdf_textarea <- parseKey(mf,"rdf_textarea")
        ; rdf_file <- parseFile(mf,"rdf_file")
        ; rdf_endpoint <- parseKey(mf,"rdf_endpoint")
        ; input_type_schema <- parseInputType(mf,"schema")
        ; schema_uri <- parseKey(mf,"schema_uri")
        ; schema_file <- parseFile(mf,"schema_file")
        ; schema_textarea <- parseKey(mf,"schema_textarea")
        ; withIncoming <- parseWithIncoming(mf)
        ; opt_iri <- parseOptIRI(mf,input_type_schema)
        )
    yield ValidationForm(
        input_type_rdf,
        rdf_uri, rdf_file, rdf_textarea, rdf_endpoint,
        input_type_schema,
        schema_uri, schema_file, schema_textarea,
        withIncoming,
        opt_iri) 
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
  
  def parseOptIRI(mf: MultipartFormData[TemporaryFile], inputType: InputType): Try[Option[IRI]] = {
    val withIRI = parseWithIRI(mf,inputType)
    withIRI match {
      case Success(true) => {
        parseIRI(mf,inputType) match {
            case Success(iri) => Success(Some(iri))
            case Failure(e) => Failure(e)
          }
      }
      case Success(false) => Success(None)
      case Failure(e) => Failure(e)  
    }
  }

  def parseWithIRI(mf: MultipartFormData[TemporaryFile], inputType: InputType): Try[Boolean] = {
    for (value <- parseKey(mf,"withIRI")) yield {
      if (value.startsWith("#noIri")) false
      else if (value.startsWith("#iri")) true
      else throw new Exception("parseWithIRI: unknown value " + value)
    }
  }

  def parseIRI(mf: MultipartFormData[TemporaryFile], inputType: InputType): Try[IRI] = {
    if (inputType == No) Failure(new Exception("Cannot obtain an IRI is input is none"))
    else
      for (value <- parseKey(mf,inputType.toString + "_iri")) yield IRI(value)
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


  def parseInputType(mf: MultipartFormData[TemporaryFile], key: String): Try[InputType] = {
    for (value <- parseKey(mf,key))
      yield {
      // The following code is to transform #XXX_key to XXX 
      val pattern = ("#(.*)_" + key).r
      val extracted = pattern.findFirstMatchIn(value).map(_ group 1)
      extracted match {
    		case Some("byUri") => ByUri
    		case Some("byFile") => ByFile
    		case Some("byInput") => ByInput
    		case Some("byEndpoint")  => ByEndpoint
    		case Some("byDereference") => ByDereference
    		case Some("no") => No
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
   }
 }
 
 def notImplementedYet[A] : Try[A] = 
   Failure(throw new Exception("Not implemented yet"))

 def getSchemaStr(vf: ValidationForm): Try[Option[String]] = {
   vf.input_type_Schema match {
     case No => Success(None)
     case ByUri => getURI(vf.schema_uri).map(str => Some(str))
     case ByFile => getFileContents(vf.schema_file).map(str => Some(str))
     case ByInput => Success(Some(vf.schema_textarea))
     case _ => failMsg("parseSchema: non supported input type: " + vf.input_type_Schema)
   }
 }

  // TODO: Move this method to a es.weso.monads.Result
  def liftTry[A](t:Try[A]):SchemaResult[A] = {
    t match {
      case Failure(e) => SchemaFailure(e.getMessage())
      case Success(v) => Passed(Stream(v))
    }
  }

  // TODO: Move this utility to other place...
  def failMsg[A](msg:String): Try[A] = {
    Failure(throw new Exception(msg))
  }

  def getURI(uri:String): Try[String] = {
    try {
      Logger.info("Trying to reach " + uri)
      val str = io.Source.fromURL(new URL(uri)).getLines().mkString("\n")
      Success(str)
    } catch {
    case e: Exception => Failure(throw new Exception("getURI: cannot retrieve content from " + uri + "\nException: " + e.getMessage))
    }
  }


  def getFileContents(opt_file: Option[File]):Try[String] = {
     opt_file match {
         case Some(file) => {
           try {
            val str = io.Source.fromFile(file).getLines().mkString("\n")
            Success(str)
           }
           catch {
             case e: Exception => Failure(throw new Exception("getFileContents: cannot retrieve content from file " + file + "\nException: " + e.getMessage))
           }
         } 
     case None => {
        Failure(new Exception("getFileContents: Input by file but no file found"))
     }
   }
  }
}

