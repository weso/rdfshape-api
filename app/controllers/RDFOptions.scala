package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import es.weso.utils._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import play.Logger


case class RDFOptions(
   syntax: RDFSyntax
 , showRDF: Boolean
 ) 
    
object RDFOptions {
  
  def availableSyntaxes : List[RDFSyntax] = {
    List(TURTLE,NTriples,RDFXML,JSONLD)
  }

  def default : RDFOptions =
    RDFOptions(TURTLE, true)
}
