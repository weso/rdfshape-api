package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.rdf._
import es.weso.utils._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import play.Logger


case class DataOptions(
   syntax: RDFSyntax
 , showData: Boolean
 ) 
    
object DataOptions {
  
  def availableSyntaxes : List[RDFSyntax] = {
    List(TURTLE,NTriples,RDFXML,JSONLD)
  }

  def default : DataOptions =
    DataOptions(TURTLE, true)
}
