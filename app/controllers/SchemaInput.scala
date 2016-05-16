package controllers

import java.io.File
import scala.util._
import es.weso.rdf._
import es.weso.schema._
import es.weso.utils.IOUtils._

case class SchemaInput(
      input_type_Schema: InputType
    , schema_uri : String
    , schema_file: Option[File]
    , schema_textarea: String
    , inputFormat: String
    , schemaName: String  
    ) {
  
  def getSchema: Try[Schema] = {
    for {
      str <- getSchemaStr
      schema <- Schemas.fromString(str,inputFormat,schemaName,None)
    } yield schema
  }
  
  def convertSchema(outputFormat: String): Try[String] = {
    for {
      str <- getSchemaStr
      schema <- Schemas.fromString(str,inputFormat,schemaName,None)
      outStr <- schema.serialize(outputFormat)
    } yield outStr
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
             , inputFormat = Schemas.defaultSchemaFormat
             , schemaName = Schemas.defaultSchemaName
             )

   def apply(schemaName: String) : SchemaInput = 
    SchemaInput( 
               input_type_Schema = ByInput
             , schema_uri = ""
             , schema_file = None
             , schema_textarea = ""
             , inputFormat = Schemas.defaultSchemaFormat
             , schemaName = schemaName
             )
    
  def apply(str: String, format: String, name: String): SchemaInput = 
    SchemaInput( 
               input_type_Schema = ByInput
        	   , schema_uri = ""
        	   , schema_file = None
        	   , schema_textarea = str
             , inputFormat = format
             , schemaName = name
        	   )
        	   
}
