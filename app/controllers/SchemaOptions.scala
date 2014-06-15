package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File

case class SchemaOptions(
      cut: Int
    , withIncoming: Boolean
    , openClosed: Boolean
    , withAny: Boolean
    , opt_iri: Option[IRI]
    ) 
    
object SchemaOptions {
  def default : SchemaOptions = 
    SchemaOptions(1, false, false, false, None)
}
