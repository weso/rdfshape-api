package es.weso.schema
import es.weso.rdf._
import es.weso.rdf.nodes._
import util._

abstract class Schema {
  
 def name: String
 
 def formats: Seq[String]
 
 def toHTML(format: String): String
 
 def validate(rdf: RDFReader): Result
 
 def validateNodeShape(node: IRI, label: String, rdf: RDFReader): Result
 
 def validateNodeAllShapes(node: IRI, rdf: RDFReader): Result
 
 def validateAllNodesAllShapes(rdf: RDFReader): Result
 
 def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Schema]
 
 def fromRDF(rdf: RDFReader): Try[Schema]
 
 def serialize(format: String): Try[String]
 
 def defaultFormat: String = formats.head
 
 def empty: Schema
 
 def shapes: List[String]
 
 def pm: PrefixMap
 
}
