package controllers

import es.weso.shex._
import es.weso.rdf._
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import es.weso.utils.IOUtils._
import util._

case class SchemaInput(
      input_type_Schema: InputType
    , schema_uri : String
    , schema_file: Option[File]
    , schema_textarea: String
    ) {

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
    SchemaInput(ByInput, "", None, "")
    
  def apply(str: String): SchemaInput = 
    SchemaInput( 
               input_type_Schema = ByInput
        	   , schema_uri = ""
        	   , schema_file = None
        	   , schema_textarea = str
        	   )
        	   
}
