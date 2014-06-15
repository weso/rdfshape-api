package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File


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

object RDFSyntax {
  def apply(syntax: String): RDFSyntax = {
    syntax match {
      case "TURTLE" => TURTLE
      case "N-TRIPLES" => NTriples
      case "RDF/XML" => RDFXML
      case "JSON-LD" => JSONLD
    }
  }
  
  def default: String = TURTLE.toString
}

sealed abstract class RDFSource
case class IRISource(iri: IRI) extends RDFSource
case class StringSource(str:String) extends RDFSource
case class FileSource(file: File) extends RDFSource
case class EndpointSource(endpoint: IRI) extends RDFSource
case object DereferenceSource extends RDFSource

case class RDFOptions(
   source : RDFSource
 , syntax: RDFSyntax
 ) 
    
object RDFOptions {

  def fromString(str:String, syntax: String = RDFSyntax.default): RDFOptions = 
    RDFOptions( source=StringSource(str) 
        	  , syntax = TURTLE
        	  )

  def fromIRI(endpoint: String, syntax : String = RDFSyntax.default) : RDFOptions =
    RDFOptions( source = IRISource(IRI(endpoint))
              , syntax = RDFSyntax(syntax)
              )

  def fromEndpoint(endpoint: String, syntax : String = RDFSyntax.default) : RDFOptions =
    RDFOptions( source = EndpointSource(IRI(endpoint))
              , syntax = RDFSyntax(syntax)
              )

  def fromDereference(syntax : String = RDFSyntax.default) : RDFOptions =
    RDFOptions( source = DereferenceSource
              , syntax = RDFSyntax(syntax)
              )
}
