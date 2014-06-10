package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File

case class ValidationForm(
      input_type_RDF: InputType
    , rdf_uri : String
    , rdf_file: Option[File]
    , rdf_textarea: String
    , rdf_endpoint: String
    , input_type_Schema: InputType
    , schema_uri : String
    , schema_file: Option[File]
    , schema_textarea: String
    , withIncoming: Boolean
    , opt_iri: Option[IRI]
    ) {
 
 // This method is used to show if form is withIRI or not
 def opt_iri_str = {
    opt_iri match {
      case Some(_) => "iri"
      case None => "noIri"
    }
 }

}
    
object ValidationForm {
  def empty : ValidationForm = 
    ValidationForm(ByInput, "", None, "", "",  
        No, "", None, "", false, None)
  
  
  def fromResult(vr:ValidationResult): ValidationForm = {
    ValidationForm(ByInput, 
        "", None, vr.str_rdf, "",
        ByInput,"",None,vr.opt_schema.getOrElse(""), 
        vr.withIncoming, vr.opt_iri)  
  }
  
}
