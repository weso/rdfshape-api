package controllers

import java.io.File
import scala.util._
import es.weso.rdf._
import es.weso.schema._
import es.weso.shex._
import es.weso.utils.IOUtils._
import es.weso.shex.SchemaFormat._

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
      case SHACL =>
        for ( str <- getSchemaStr
            ; (schema,pm) <- {
                 val result = Schema.fromString(str,inputFormat)
         println("ConverSchema: Result = " + result)
         result
        }
        ) yield schema.serialize(outputFormat)
      case _ => {
        throw new Error(s"convertSchema: Unsupported schemaVersion: " + schemaVersion)
      }
    }
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
             , inputFormat = SchemaFormat.default.name
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
