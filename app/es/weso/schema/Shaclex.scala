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
  
  override def validate(rdf: RDFReader) : Result = {
    val validator = CoreValidator(schema)
    
    val r = validator.validate(rdf)
    checked2Result(r)
  }
  
  override def validateNodeShape(node: IRI, shape: String, rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateNodesShape for SHACLex yet")
  }
  
  override def validateNodeAllShapes(node: IRI, rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateNodesAllShapes for SHACLex yet")
  }
  
  override def validateAllNodesAllShapes(rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateAllNodesAllShapes for SHACL yet")
  }
  
  def checked2Result(result: Checked[ShaclSchema,ConstraintReason,ConstraintError]): Result = {
    println(result)
    val isValid = result.isOK
    val msg = 
      if (result.isOK) s"Valid. Reason: ${result.reasons}"
      else "Not Valid" 
    val solutions: Seq[Solution] = Seq()
    val errors: Seq[ErrorInfo] = result.errors.map(constraintError2ErrorInfo(_))
    Result(isValid,msg,solutions,errors)
  }
  
  def constraintError2ErrorInfo(ce: ConstraintError): ErrorInfo = {
    ErrorInfo(ce.toString)
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
  
  override def shapes: List[String] = {
    schema.shapes.map(_.id).filter(_.isDefined).map(_.get).map(_.toString).toList 
  }
  
  override def pm: PrefixMap = PrefixMap.empty // TODO: Improve this to add pm to Shaclex
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
