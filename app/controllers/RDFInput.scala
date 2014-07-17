package controllers

import es.weso.shex._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import util._
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import es.weso.rdf.RDF
import views.html.helper.input
import es.weso.rdf.reader.RDFFromWeb
import es.weso.rdf.reader.Endpoint
import es.weso.utils.RDFSyntax


case class RDFInput(
      input_type_RDF: InputType
    , rdf_uri : String
    , rdf_file: Option[File]
    , rdf_textarea: String
    , rdf_endpoint: String
    ) {
  
  def getRDFStr(): Try[String] = 
   input_type_RDF match {
     case ByUri => getURI(rdf_uri)
     case ByFile => getFileContents(rdf_file)
     case ByInput => Success(rdf_textarea)
     case ByEndpoint => Success("<<Endpoint: " + rdf_endpoint + ">>") 
     case ByDereference => Success("<<Web Dereference>>")
     case _ => throw new Exception("get_RDFString: Unknown input type")
  }
  
  def getRDF(syntax: RDFSyntax) : Try[RDF] = {
   input_type_RDF match {
     case ByUri | ByFile | ByInput => 
       			 for ( str <- getRDFStr
                     ; pair <- RDFParse(str,syntax)
                     ) yield pair._1
     case ByEndpoint => 
       if (rdf_endpoint == "") {
         Failure(throw new Exception("Endpoint URI must be non-empty"))
       } else {
       // Check that it is a well formed URI before creating RDF endpoint
       val cnv = Try(new java.net.URI(rdf_endpoint))
       cnv.map(_ => Endpoint(rdf_endpoint)) 
       }
     case ByDereference => Success(RDFFromWeb())
     case _ => Failure(throw new Exception("getRDF: Unknown input type"))
  }
  }
}
    
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
