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
    , trigger: ValidationTrigger
    , showSchema: Boolean
    ) {
  
  def extract_iri_str : String = {
    trigger.extractNode
  }

  def maybeFocusNode: Option[String] = {
    trigger.maybeFocusNode
  }
  
  def maybeShape: Option[String] = {
    trigger.maybeShape
  }

  
  def opt_iri: Option[IRI] = {
    maybeFocusNode.map(IRI(_))
  }
  
  def node: String = {
    trigger match {
      case NodeShape(n,s) => n.toString
      case NodeAllShapes(n) => n.toString
      case _ => ""
    }
  }
  
  def shape: String = {
    trigger match {
      case NodeShape(n,s) => s.toString
      case _ => ""
    }
    
  }
}
    
object SchemaOptions {

  // TODO: read these values from properties file
  lazy val DEFAULT_CUT = 1
  lazy val DEFAULT_OptIRI = None
  lazy val DEFAULT_ShowSchema = true
  
  lazy val availableFormats: List[String] = 
    Schemas.availableFormats

  def default : SchemaOptions = 
    SchemaOptions(
        DEFAULT_CUT, 
        ValidationTrigger.default,
        DEFAULT_ShowSchema)
    
  def defaultWithNode(iri: String): SchemaOptions = 
    default.copy(trigger = ValidationTrigger.nodeAllShapes(iri)) 
    
  def fromSchemaInput(schemaInput: SchemaInput): SchemaOptions = {
    default.copy(
        showSchema = true 
    )
  }
}
