package es.weso.schema
import es.weso.rdf._
import es.weso.rdf.nodes._
import util._

abstract class Schema {
  
 def name: String
 
 def formats: Seq[String]
 
 def toHTML(format: String): String
 
 def validateRDF(rdf: RDFReader): Result
 
 def validateNodeAllLabels(node: RDFNode, rdf: RDFReader): Result
 
 def validateAllNodesAllLabels(rdf: RDFReader): Result
 
 def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Schema]
 
 def fromRDF(rdf: RDFReader): Try[Schema]
 
 def serialize(format: String): Try[String]
 
 def defaultFormat: String = formats.head
 
 def empty: Schema
 
}
