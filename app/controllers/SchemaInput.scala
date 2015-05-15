package controllers

import es.weso.shex.{Schema => ShexSchema}
import es.weso.shacl._
import es.weso.rdf._
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import es.weso.utils.IOUtils._
import util._
import es.weso.utils.SchemaUtils

case class SchemaInput(
      input_type_Schema: InputType
    , schema_uri : String
    , schema_file: Option[File]
    , schema_textarea: String
    , inputFormat: String
    , schemaVersion: SchemaVersion  
    ) {
  
  def convertSchema(outputFormat: String): Try[String] = {
    schemaVersion match {
      case SHEX_Deriv =>
        for ( str <- getSchemaStr
        ; (schema,pm) <- ShexSchema.fromString(str,inputFormat)
        ) yield schema.serialize(outputFormat)
      case SHACL_Deriv =>
        for ( str <- getSchemaStr
        ; (schema,pm) <- Schema.fromString(str,inputFormat)
        ) yield schema.serialize(outputFormat)
    }
  }

  // TODO: remove format parameter
  def getSchema(format: String): Try[ShexSchema] = {
    for ( str <- getSchemaStr
        ; (schema,pm) <- ShexSchema.fromString(str,format)
        ) yield schema
  }
  
  def getSchemaStr: Try[String] = {
   input_type_Schema match {
     case ByUri => if (schema_uri == "") 
    	 			Failure(throw new Exception("Empty URI"))
    	 		   else getURI(schema_uri)
     case ByFile => getFileContents(schema_file)
     case ByInput => Success(schema_textarea)
     case _ => Failure(throw new Exception("get_SchemaString: Unsupported input type"))
   }
  }
  
  def extract_str() : String = {
    this.getSchemaStr.getOrElse("")
  }
}
    
object SchemaInput {
  def apply() : SchemaInput = 
    SchemaInput( 
               input_type_Schema = ByInput
             , schema_uri = ""
             , schema_file = None
             , schema_textarea = ""
             , inputFormat = SchemaUtils.defaultSchemaFormat
             , schemaVersion = SchemaVersions.default
             )
    
  def apply(str: String, format: String, version: String): SchemaInput = 
    SchemaInput( 
               input_type_Schema = ByInput
        	   , schema_uri = ""
        	   , schema_file = None
        	   , schema_textarea = str
             , inputFormat = format
             , schemaVersion = SchemaVersions.get(version)
        	   )
        	   
}
