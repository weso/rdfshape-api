package controllers

import es.weso.monads._
import es.weso.rdf._
import xml.Utility.escape
import es.weso.rdf.nodes.RDFNode
import es.weso.rdf.nodes.IRI
import java.io.File
import es.weso.schema._

case class SchemaOptions(
      cut: Int
    , opt_iri: Option[IRI]
    , showSchema: Boolean
    ) {
  
  def extract_iri_str : String = {
    opt_iri.map(_.str).getOrElse("")
  }
  
  
}
    
object SchemaOptions {

  // TODO: read these values from properties file
  lazy val DEFAULT_CUT = 1
  lazy val DEFAULT_OptIRI = None
  lazy val DEFAULT_ShowSchema = true
  
  // TODO....Change this!!!
  lazy val availableFormats: List[String] = 
    Schemas.availableFormats

  def default : SchemaOptions = 
    SchemaOptions(
        DEFAULT_CUT, 
        DEFAULT_OptIRI,
        DEFAULT_ShowSchema)
    
  def defaultWithIri(iri: String): SchemaOptions = 
    default.copy(opt_iri = Some(IRI(iri))) 
    
/*  def defaultWithFormat(format: String): SchemaOptions = 
    default.copy(format = format) */
    
  def fromSchemaInput(schemaInput: SchemaInput): SchemaOptions = {
    default.copy(
        // format = schemaInput.inputFormat, 
        showSchema = true 
    )
  }
}
