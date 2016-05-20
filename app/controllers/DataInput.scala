package controllers

import es.weso.rdf.PrefixMap
import xml.Utility.escape
import es.weso.rdf.nodes._
import java.io.File
import util.{Try, Success => TrySuccess, Failure => TryFailure}
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import es.weso.rdf._
import views.html.helper.input
import es.weso.rdf.jena._
import es.weso.utils._
import es.weso.shex.DataFormat
import es.weso.rdf.jena.RDFAsJenaModel


case class DataInput(
      input_type_Data: InputType
    , data_uri : String
    , data_file: Option[File]
    , data_textarea: String
    , data_endpoint: String = ""
    , dataFormat: String
    ) {
  
  def getDataStr(): Try[String] = 
   input_type_Data match {
     case ByUri => getURI(data_uri)
     case ByFile => getFileContents(data_file)
     case ByInput => TrySuccess(data_textarea)
     case ByEndpoint => TrySuccess("<<Endpoint: " + data_endpoint + ">>") 
     case ByDereference => TrySuccess("<<Web Dereference>>")
     case _ => throw new Exception("get_DataStr: Unknown input type")
  }
  
  def getData(format: String, rdfs:Boolean) : Try[RDFReader] = {
   input_type_Data match {
     case ByUri | ByFile | ByInput => 
       			 for ( str <- getDataStr
                 ; rdf <- parseStrAsRDFReader(str,format,rdfs)
                 ) yield rdf
     case ByEndpoint => 
       if (data_endpoint == "") {
         TryFailure(throw new Exception("Endpoint URI must be non-empty"))
       } else {
       // Check that it is a well formed URI before creating RDF endpoint
       val cnv = Try(new java.net.URI(data_endpoint))
       cnv.map(_ => Endpoint(data_endpoint)) 
       }
     case ByDereference => TrySuccess(RDFFromWeb())
     case _ => TryFailure(throw new Exception("getData: Unknown input type"))
  }
  }
  
  def extract_str : String = this.getDataStr().getOrElse("")
  
  def convertData(outputFormat: String): Try[String] = {
    for {
      str <- getDataStr
      rdf <- RDFAsJenaModel.fromChars(str,dataFormat)
    } yield rdf.serialize(outputFormat)
  }

}
    
object DataInput {
  def apply() : DataInput = 
    DataInput(ByInput, "", None, "","",DataFormat.default.name)
    
  def apply(str: String): DataInput = 
    DataInput( input_type_Data = ByInput
        	, data_uri = ""
        	, data_file = None
        	, data_textarea = str
        	, data_endpoint = ""
        	, DataFormat.default.name)
}
