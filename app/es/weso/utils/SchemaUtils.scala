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

sealed abstract class SchemaSyntax 

case object SHEXC extends SchemaSyntax {
  override def toString = "SHEXC"
}
case class RDFSchema(rdfSyntax: RDFSyntax) extends SchemaSyntax {
  override def toString = rdfSyntax.toString
}

object SchemaSyntax {
  
  def apply(syntax: String): SchemaSyntax = {
    syntax match {
      case "SHEXC"  => SHEXC
      case s => RDFSchema(RDFSyntax(s))
   }
  }
    
  def apply(syntax: Option[String]): SchemaSyntax = {
    syntax match {
      case Some(s) => SchemaSyntax(s)
      case None => RDFSchema(RDFSyntax("Unknown"))
    }
  }
  
  def default: String = TURTLE.toString
}


object SchemaUtils {

/*  def schemaParse(str: String, syntax: SchemaSyntax): Try[(Schema,String)] = {
    syntax match {
/*      case SHEXC => {
        // Todo: simplify the following... 
         Try{ 
           RDFTriples.parse(str) match {
         	case Success(rdf) => 
         	  (rdf,str)
         	case Failure(e) =>
         	  throw new Exception("Exception :" + e.getMessage)
           }
         }
      } */
      case _ => 
        Failure(throw new Exception("Not implemented parser for syntax" + syntax + " yet"))
    }
 }
 */

}