package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File

case class SchemaInput(
      input_type_Schema: InputType
    , schema_uri : String
    , schema_file: Option[File]
    , schema_textarea: String
    ) 
    
object SchemaInput {
  def apply() : SchemaInput = 
    SchemaInput(ByInput, "", None, "")
    
  def apply(str: String): SchemaInput = 
    SchemaInput( input_type_Schema = ByInput
        	   , schema_uri = ""
        	   , schema_file = None
        	   , schema_textarea = str
        	   )
}
