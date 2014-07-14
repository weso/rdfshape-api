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
      rdfInput: RDFInput
    , rdfOptions: RDFOptions
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
  def apply(): ValidationForm = 
    ValidationForm(
          rdfInput = RDFInput()
        , rdfOptions = RDFOptions.default
        , withSchema = false
        , schemaInput = SchemaInput()
        , schemaOptions = SchemaOptions.default
        )
        
  def fromResult(vr:ValidationResult): ValidationForm = {
    ValidationForm(
      rdfInput = RDFInput(vr.str_rdf)
    , rdfOptions = vr.opts_rdf
    , withSchema = vr.withSchema
    , schemaInput = SchemaInput(vr.str_schema)
    , schemaOptions = vr.opt_schema 
    )
  }
  
}
