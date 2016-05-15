package es.weso.schema
import es.weso.rdf.nodes._

abstract class ValidationTrigger {
  def explain: String
  def extractNode: String = 
    maybeFocusNode.getOrElse("")
    
  def extractShape: String = 
    maybeShape.getOrElse("")
    
  def maybeFocusNode: Option[String] 
  def maybeShape: Option[String]
} 

/**
 * Validates only scope declarations
 */
case object ScopeDeclarations extends ValidationTrigger {
  override def explain = "Only scope node declarations"
  
  override def maybeFocusNode = None
  override def maybeShape = None
}

/**
 * Validates all nodes against all shapes
 * 
 */
case object AllNodesAllShapes extends ValidationTrigger {
  override def explain = "All nodes in data against all shapes"
  override def maybeFocusNode = None
  override def maybeShape = None
}

/**
 * Validates a node against all shapes
 * 
 */
case class NodeAllShapes(node: RDFNode) extends ValidationTrigger {
  override def explain = "A node with all shapes"
  override def maybeFocusNode = Some(node.toString)
  override def maybeShape = None
}

/**
 * Validates a node against a specific shape
 */
case class NodeShape(node: RDFNode, shape: RDFNode) extends ValidationTrigger {
  override def explain = "A node with a shape"
  override def maybeFocusNode = Some(node.toString)
  override def maybeShape = Some(shape.toString)
}

object ValidationTrigger {
  
 lazy val default: ValidationTrigger = ScopeDeclarations
 
 // Validation Trigger constructors (could replace by apply)
 def nodeAllShapes(node: String): ValidationTrigger = 
   NodeAllShapes(IRI(node))
   
 def nodeShape(node: String, shape: String): ValidationTrigger = 
   NodeShape(IRI(node), IRI(shape))
 
 lazy val scopeDeclarations: ValidationTrigger = ScopeDeclarations
 
 lazy val allNodesAllShapes: ValidationTrigger = AllNodesAllShapes

 def fromOptIRI(optIRI: Option[String]): ValidationTrigger = {
   optIRI match {
     case None => default
     case Some(iri) => nodeAllShapes(iri)
   }
 }
}