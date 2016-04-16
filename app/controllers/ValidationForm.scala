package controllers

import es.weso.monads._
import es.weso.rdf._
import xml.Utility.escape
import es.weso.rdf.nodes.RDFNode
import es.weso.rdf.nodes.IRI
import java.io.File
//import es.weso.utils.JenaUtils._
import es.weso.utils.IOUtils._
import scala.util.Try
import scala.util.Success

case class ValidationForm(
      dataInput: DataInput
    , dataOptions: DataOptions
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

/* private def extract_str : String = {
   // (this.getSchemaStr().map(opt => opt.getOrElse(""))).getOrElse("")
   dataInput.extract_str
 } */

 private def getSchemaOptions(): Try[Option[SchemaOptions]] = {
   if (withSchema) {
     Success(Some(schemaOptions))
   } else 
     Success(None)
 }
 
 def input_type_Schema_str : String = {
   schemaInput.input_type_Schema.toString
 } 

 // Public methods
 
 def dataStr : String = {
   dataInput.extract_str
 }
 
  def dataFormat : String = {
   dataOptions.format
 }

 def schemaStr : String = {
   getSchemaStr.map(opt => opt.getOrElse("")).getOrElse("")
 }
 
 def schemaFormat: String = {
   schemaInput.inputFormat
 }
 
 def schemaVersion : String = {
   schemaInput.schemaVersion.versionName
 }

 def focusNode : String = {
   schemaOptions.opt_iri.map(_.str).getOrElse("")
 }

 def maybeFocusNode : Option[String] = {
   schemaOptions.opt_iri.map(_.str)
 }
 
}
    
object ValidationForm {
  def apply(): ValidationForm = 
    ValidationForm(
          dataInput = DataInput()
        , dataOptions = DataOptions.default
        , withSchema = false
        , schemaInput = SchemaInput()
        , schemaOptions = SchemaOptions.default
        )
        
  def fromResult(vr:ValidationResult): ValidationForm = {
    ValidationForm(
      dataInput = DataInput(vr.dataStr)
    , dataOptions = vr.dataOptions
    , withSchema = vr.withSchema
    , schemaInput = SchemaInput(vr.schemaStr,vr.schemaFormat, vr.schemaVersion)
    , schemaOptions = vr.schemaOptions
    )
  }
  
  def fromDataConversion(data: String, format: String): ValidationForm = {
    ValidationForm(
      dataInput = DataInput(data)
    , dataOptions = DataOptions(format,true)
    , withSchema = false
    , schemaInput = SchemaInput()
    , schemaOptions = SchemaOptions.default
    )
  }
  
  def fromSchemaConversion(schemaInput: SchemaInput): ValidationForm = {
    ValidationForm(
      dataInput = DataInput()
    , dataOptions = DataOptions.default
    , withSchema = true
    , schemaInput = schemaInput
    , schemaOptions = SchemaOptions.fromSchemaInput(schemaInput)
    )
  }

}
