package es.weso.utils

import java.net.URL
import play.Logger
import java.io.File
import scala.util._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf._
import es.weso.shex.DataFormat

object RDFUtils {

  def parseStrAsRDFReader(str: String, format: String, rdfs: Boolean): Try[RDFReader] = {
    val trymodel = RDFAsJenaModel.fromChars(str,format)
    if (rdfs) {
      val model = trymodel.get
      Success(model.extend_rdfs)
    }
    else trymodel
  }

  def defaultDataFormat = "TURTLE"
  
  def getFormat(syntax: Option[String]): String = {
    syntax match {
      case Some(s) => 
        if (DataFormat.available(s)) s
        else // TODO: Check a better failure... 
          throw new Exception("Unsupported syntax " + s)
      case None => defaultDataFormat
    }
    
  }
}