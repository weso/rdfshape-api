package es.weso.utils

import java.net.URL
import play.Logger
import scala.util.Failure
import java.io.File
import scala.util.Try
import scala.util.Success
import es.weso.rdf.reader.RDFFromJenaModel
import es.weso.rdf.RDFTriples
import es.weso.rdf.RDF

sealed abstract class RDFSyntax 

case object TURTLE extends RDFSyntax {
  override def toString = "TURTLE"
}
case object NTriples extends RDFSyntax {
  override def toString = "N-TRIPLES"
}
case object RDFXML extends RDFSyntax {
  override def toString = "RDF/XML"
}
case object JSONLD extends RDFSyntax {
  override def toString = "JSON-LD"
}
case object Unknown extends RDFSyntax {
  override def toString = "<unknown>"
}

object RDFSyntax {
  
  def apply(syntax: String): RDFSyntax = {
    syntax match {
      case "TURTLE" => TURTLE
      case "N-TRIPLES" => NTriples
      case "RDF/XML" => RDFXML
      case "JSON-LD" => JSONLD
      case _ => {
        Logger.error("Unsupported RDF Syntax: " + syntax)
        TURTLE
      }
   }
  }
    
  def apply(syntax: Option[String]): RDFSyntax = {
    syntax match {
      case Some(s) => RDFSyntax(s)
      case None => Unknown
    }
  }
  
  def default: String = TURTLE.toString
}


object RDFUtils {

  def RDFParse(str: String, syntax: RDFSyntax): Try[(RDF,String)] = {
    syntax match {
      case TURTLE => {
        // Todo: simplify the following... 
         Try{ 
           RDFTriples.parse(str) match {
         	case Success(rdf) => 
         	  (rdf,str)
         	case Failure(e) =>
         	  throw new Exception("Exception :" + e.getMessage)
           }
         }
      }
      case _ => 
        Failure(throw new Exception("Not implemented parser for syntax" + syntax + " yet"))
    }
 }

}