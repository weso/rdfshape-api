package es.weso.schema
import es.weso.shacl.ShaclBinder
import es.weso.schema.shacl_tq._
import es.weso.shex.DataFormat
import es.weso.rdf.RDFReader
import es.weso.rdf.nodes._
import es.weso.rdf.jena._
import es.weso.validating._
import util._
import com.hp.hpl.jena.rdf.model.Model
import es.weso.validating.Checked._
import es.weso.rdf.PREFIXES.{rdf_type}
import es.weso.utils.TryUtils
import es.weso.rdf.PrefixMap

case class Shacl_TQ(
    binder: ShaclBinder) extends Schema {
  
  lazy val sh = IRI("http://www.w3.org/ns/shacl#")
  lazy val sh_ValidationResult = sh + "ValidationResult"
  lazy val sh_message = sh + "message"

  
  override def name = "SHACL_TQ"
  
  override def formats = DataFormat.formatNames
  
  override def toHTML(format: String): String = {
    "<pre>" + binder.serialize(format) + "</pre>"
  }
  
  override def validate(rdf: RDFReader) : Result = {
    println("Validating RDF with SHACL_TQ")
    val result: Model = binder.validateModel(rdf)
    val checked: Checked[Boolean,ConstraintReason,ViolationError] = convertResultModel(result)
    checked2Result(checked)
  }
  
  def convertResultModel(resultModel: Model): Checked[Boolean,ConstraintReason,ViolationError] = {
    if (resultModel.size == 0) ok(SingleReason(true,"Validated"))
    else {
      val result = RDFAsJenaModel(resultModel)
      val ts = result.triplesWithPredicateObject(rdf_type,sh_ValidationResult)
      val vs = ts.map(_.subj).map(s => getViolationError(result,s)).toSeq
      val violationErrors = TryUtils.filterSuccess(vs)
      violationErrors match {
        case Success(es) => errs(es)
        case Failure(e) => checkError(ViolationError.msgError(e.getMessage))
      }
    }
  }
  
  private def getViolationError(result: RDFReader, node: RDFNode): Try[ViolationError] = {
    println(s"getViolationError on $node")
    val ts = result.triplesWithSubjectPredicate(node,sh_message)
    val msg = 
      if (ts.size == 1) ts.head.obj.toString
      else "<not found message>"
      
    // ViolationError.parse(result,node)
    // ViolationErrorParser.parse(node,result)
    ViolationError.parse(result,node)
    // Success(ViolationError.msgError(msg))
  }

  override def validateNodeShape(node: IRI, shape: String, rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateNodesLabels for SHACL TQ")
  }
  
  override def validateNodeAllShapes(node: IRI, rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateAllNodesAllLabels for SHACL TQ")
  }
  
  override def validateAllNodesAllShapes(rdf: RDFReader) : Result = {
    throw new Exception("Not implemented validateAllNodesAllLabels for SHACL TQ")
  }
  
  def checked2Result(result: Checked[Boolean,ConstraintReason,ViolationError]): Result = {
    println(s"checked2Result: $result")
    val isValid = result.isOK
    val msg = 
      if (result.isOK) s"Valid. Reason: ${result.reasons}"
      else s"Not Valid ${result.errors}" 
    val solutions: Seq[Solution] = Seq()
    val errors: Seq[ErrorInfo] = result.errors.map(violationError2ErrorInfo(_))
    Result(isValid,msg,solutions,errors)
  }

  def errResult(msg: String): Result = {
    Result(false,msg,Seq(),Seq())  
  }
  
  def violationError2ErrorInfo(ve: ViolationError): ErrorInfo = {
    ErrorInfo(ve.toHTMLRow)
  }
  
  override def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Schema] = {
    val b : ShaclBinder = binder.fromString(cs,format,base)
    val s : Schema = Shacl_TQ(b)  
    Success(s)
  }
  
  override def fromRDF(rdf: RDFReader): Try[Schema] = {
    val b: ShaclBinder = ShaclBinder.fromRDF(rdf)
    val s : Schema = Shacl_TQ(b)  
    Success(s)
  }
  
  override def serialize(format: String): Try[String] = {
    Success(binder.serialize(format)) 
  }
  
  override def empty: Schema = Shacl_TQ.empty
  
  override def shapes: List[String] = List()
  
  override def pm: PrefixMap = PrefixMap.empty // TODO: Improve this adding pm to ShaclBinder
}

object Shacl_TQ {
  def empty: Shacl_TQ = Shacl_TQ(binder = ShaclBinder.empty)
  
  def fromString(cs: CharSequence, format: String, base: Option[String]): Try[Shacl_TQ] = { 
    val b : ShaclBinder = ShaclBinder.fromString(cs,format,base)
    val s = Shacl_TQ(b)  
    Success(s)
  }
  
}
