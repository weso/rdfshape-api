package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.rdf._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import java.io.File
import es.weso.shacl.SchemaFormats

case class SchemaOptions(
      format: String
    , cut: Int
    , withIncoming: Boolean
    , withAny: Boolean
    , opt_iri: Option[IRI]
    , showSchema: Boolean
    ) {
  
  def extract_iri_str : String = {
    opt_iri.map(_.str).getOrElse("")
  }
  
  
}
    
object SchemaOptions {

  // TODO: read these values from properties file
  lazy val defaultCut = 10
  lazy val defaultWithIncoming = false
  lazy val defaultWithAny = false
  lazy val defaultOptIRI = None
  lazy val defaultShowSchema = true
  
  lazy val availableFormats: List[String] = 
    SchemaFormats.toList

  def default : SchemaOptions = 
    SchemaOptions("SHEXC",
        defaultCut, 
        defaultWithIncoming, 
        defaultWithAny, 
        defaultOptIRI,
        defaultShowSchema)
    
  def defaultWithIri(iri: String): SchemaOptions = 
    default.copy(opt_iri = Some(IRI(iri))) 
    
  def defaultWithFormat(format: String): SchemaOptions = 
    default.copy(format = format)
    
  def fromSchemaInput(schemaInput: SchemaInput): SchemaOptions = {
    default.copy(
        format = schemaInput.inputFormat, 
        showSchema = true 
    )
  }
}
