package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File

case class RDFInput(
      input_type_RDF: InputType
    , rdf_uri : String
    , rdf_file: Option[File]
    , rdf_textarea: String
    , rdf_endpoint: String
    ) 
    
object RDFInput {
  def apply() : RDFInput = 
    RDFInput(ByInput, "", None, "","")
    
  def apply(str: String): RDFInput = 
    RDFInput( input_type_RDF = ByInput
        	, rdf_uri = ""
        	, rdf_file = None
        	, rdf_textarea = str
        	, rdf_endpoint = ""
        	)
}
