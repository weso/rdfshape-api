package es.weso.utils

import java.net.URL
import play.Logger
import java.io.File
import scala.util._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf._
import es.weso.shacl.DataFormats

object RDFUtils {

  def RDFParse(str: String, format: String): Try[RDFReader] = {
    RDFAsJenaModel.fromChars(str,format) 
  }

  def defaultDataFormat = "TURTLE"
  
  def getFormat(syntax: Option[String]): String = {
    syntax match {
      case Some(s) => 
        if (DataFormats.available(s)) s
        else // TODO: Check a better failure... 
          throw new Exception("Unsupported syntax " + s)
      case None => defaultDataFormat
    }
    
  }
}