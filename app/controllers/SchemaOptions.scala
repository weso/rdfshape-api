package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.rdf._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File

case class SchemaOptions(
      cut: Int
    , withIncoming: Boolean
    , withAny: Boolean
    , opt_iri: Option[IRI]
    , showSchema: Boolean
    ) 
    
object SchemaOptions {
  def default : SchemaOptions = 
    SchemaOptions(10, false, false, None,true)
}
