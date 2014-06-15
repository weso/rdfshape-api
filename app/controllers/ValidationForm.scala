package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import es.weso.utils.IOUtils._
import scala.util.Try
import scala.util.Success

case class ValidationForm(
      input_type_RDF: InputType
    , rdf_uri : String
    , rdf_file: Option[File]
    , rdf_textarea: String
    , rdf_endpoint: String
    , withSchema : Boolean
    , schemaInput: SchemaInput
    , schemaOptions: SchemaOptions
    ) {
 
 // This method is used to show if form is withIRI or not in index.scala.html
 // the values must match the prefix of values in tabs.js
 def opt_iri_str = {
   schemaOptions.opt_iri match {
      	case Some(_) => "iri"
      	case None => "noIri"
    }
 }
 
 // this method is used in index.scala.html to show if schema is toggled or no
 // the values must match the values in tabs.js
 def schema_toggle = {
   if (withSchema) "#schema"
   else "#no_schema"
 }

 def getSchemaStr(): Try[Option[String]] = {
   if (withSchema) {
	   schemaInput.input_type_Schema match {
      	case ByUri => getURI(schemaInput.schema_uri).map(str => Some(str))
      	case ByFile => getFileContents(schemaInput.schema_file).map(str => Some(str))
      	case ByInput => Success(Some(schemaInput.schema_textarea))
      	case _ => failMsg("parseSchema: non supported input type: " + schemaInput.input_type_Schema)
      }     
   }
   else Success(None)
 }

 def getSchemaOptions(): Try[Option[SchemaOptions]] = {
   if (withSchema) {
     Success(Some(schemaOptions))
   } else 
     Success(None)
 }
 
 def input_type_Schema_str : String = {
   schemaInput.input_type_Schema.toString
 } 

}
    
object ValidationForm {
  def empty : ValidationForm = 
    ValidationForm(
          input_type_RDF = ByInput
        , rdf_uri = ""
        , rdf_file = None
        , rdf_textarea = ""
        , rdf_endpoint = ""
        , withSchema = false
        , schemaInput = SchemaInput()
        , schemaOptions = SchemaOptions.default
        )
        
  def fromResult(vr:ValidationResult): ValidationForm = {
    val has_schema = vr.withSchema
    val input_schema = if (has_schema) 
    					SchemaInput(vr.str_schema) 
    				 else 
    				    SchemaInput()
    				    
    val opts_schema = if (has_schema) 
    					vr.opt_schema 
    				  else 
    				    SchemaOptions.default
    
    ValidationForm(
      input_type_RDF = ByInput
    , rdf_uri = ""
    , rdf_file = None
    , rdf_textarea = vr.str_rdf
    , rdf_endpoint = ""
    , withSchema = has_schema
    , schemaInput = input_schema
    , schemaOptions = opts_schema
    )
  }
  
}
