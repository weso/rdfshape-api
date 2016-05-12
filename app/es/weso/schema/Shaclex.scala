package es.weso.schema
import es.weso.rdf._
import es.weso.rdf.nodes._
import es.weso.rdf.jena.RDFAsJenaModel
import org.scalactic._
import es.weso.shacl.{Schema => ShaclSchema, _}
import es.weso.shex.DataFormat
import es.weso.validating._
import util._

case class Shaclex(schema: ShaclSchema) extends Schema {
  override def name = "SHACLex"
  
  override def formats = DataFormat.formatNames
  
  override def toHTML(format: String): String = {
    "<pre>" + schema.serialize(format) + "</pre>"
  }
  
  override def validateRDF(rdf: RDFReader) : Result = {
    val validator = CoreValidator(schema)
    
    val r = validator.validate(rdf)
    checked2Result(r)
  }
  
  override def validateNodeAllLabels(node: RDFNode, rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateAllNodesAllLabels for SHACL")
  }
  
  override def validateAllNodesAllLabels(rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateAllNodesAllLabels for SHACL")
  }
  
  def checked2Result(result: Checked[ShaclSchema,ConstraintReason,ConstraintError]): Result = {
    println(result)
    throw new Exception("Not implemented result conversion " + result + " yet")
  }
  
  override def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Schema] = {
    for { 
      rdf <- RDFAsJenaModel.fromChars(cs,format,base)
      (schema,pm) <- RDF2Shacl.getShacl(rdf)
    } yield Shaclex(schema)
  }
  
  override def fromRDF(rdf: RDFReader): Try[Schema] = {
    for {
      (schema,pm) <- RDF2Shacl.getShacl(rdf)
    } yield Shaclex(schema)
  }
  
  override def serialize(format: String): Try[String] = {
    if (formats.contains(format))
      schema.serialize(format)
    else
      Failure(throw new Exception(s"Format $format not supported to serialize $name. Supported formats=$formats")) 
  }
  
  override def empty: Schema = Shaclex.empty
  
}

object Shaclex {
  def empty: Shaclex = Shaclex(schema = ShaclSchema.empty)
  
  def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Shaclex] = { 
    for { 
      rdf <- RDFAsJenaModel.fromChars(cs,format,base)
      (schema,pm) <- RDF2Shacl.getShacl(rdf)
    } yield Shaclex(schema)
  }
  
}
