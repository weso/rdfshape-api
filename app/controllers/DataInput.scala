package controllers

import es.weso.shex._
import es.weso.rdf.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes._
import java.io.File
import util._
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import es.weso.rdf._
import views.html.helper.input
import es.weso.rdf.jena._
import es.weso.utils._


case class DataInput(
      input_type_Data: InputType
    , data_uri : String
    , data_file: Option[File]
    , data_textarea: String
    , data_endpoint: String = ""
    ) {
  
  def getDataStr(): Try[String] = 
   input_type_Data match {
     case ByUri => getURI(data_uri)
     case ByFile => getFileContents(data_file)
     case ByInput => Success(data_textarea)
//     case ByEndpoint => Success("<<Endpoint: " + data_endpoint + ">>") 
     case ByDereference => Success("<<Web Dereference>>")
     case _ => throw new Exception("get_DataStr: Unknown input type")
  }
  
  def getData(format: String) : Try[RDFReader] = {
   input_type_Data match {
     case ByUri | ByFile | ByInput => 
       			 for ( str <- getDataStr
                 ; rdf <- RDFParse(str,format)
                 ) yield rdf
     case ByEndpoint => 
       if (data_endpoint == "") {
         Failure(throw new Exception("Endpoint URI must be non-empty"))
       } else {
       // Check that it is a well formed URI before creating RDF endpoint
       val cnv = Try(new java.net.URI(data_endpoint))
       cnv.map(_ => Endpoint(data_endpoint)) 
       }
     case ByDereference => Success(RDFFromWeb())
     case _ => Failure(throw new Exception("getData: Unknown input type"))
  }
  }
  
  def extract_str : String = {
    this.getDataStr().getOrElse("")
  }
}
    
object DataInput {
  def apply() : DataInput = 
    DataInput(ByInput, "", None, "","")
    
  def apply(str: String): DataInput = 
    DataInput( input_type_Data = ByInput
        	, data_uri = ""
        	, data_file = None
        	, data_textarea = str
        	, data_endpoint = ""
        	)
}
