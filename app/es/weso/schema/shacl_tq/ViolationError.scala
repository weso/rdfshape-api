package es.weso.schema.shacl_tq

import es.weso.rdf.nodes._
import es.weso.rdf.{RDFReader, PrefixMap}
import es.weso.utils.PrefixMapUtils
import util._
import xml.Utility.escape
import es.weso.schema.Shacl_TQ._

case class ViolationError(
    message: String,
    focusNode: Option[RDFNode],
    subject: Option[RDFNode],
    predicate: Option[RDFNode],
    severity: Option[RDFNode],
    sourceConstraint: Option[RDFNode],
    sourceShape: Option[RDFNode],
    sourceTemplate: Option[RDFNode]
) {
  
 override def toString: String = {
    s"*** Error: $message\n" +
    maybe("focusNode", focusNode) + 
    maybe("subject", subject) +
    maybe("predicate", predicate) + 
    maybe("severity", severity) +
    maybe("sourceConstraint", sourceConstraint) +
    maybe("sourceShape", sourceShape) +
    maybe("sourceTemplate", sourceTemplate) 
  }

  def toHTMLRow(pm:PrefixMap): String = {
    "<tr class=\"error\"><td class=\"message\">" + message + "</td>" +
    maybeNodeHtml("focusNode", focusNode,pm) + 
    maybeNodeHtml("subject", subject,pm) + 
    maybeNodeHtml("predicate", predicate,pm) + 
    maybeNodeHtml("severity", severity,pm) + 
    maybeNodeHtml("sourceConstraint", sourceConstraint,pm) + 
    maybeNodeHtml("sourceShape", sourceShape,pm) + 
    maybeNodeHtml("sourceTemplate", sourceTemplate,pm)  
  }

  private def maybe[A](name: String, m: Option[A]) = {
    if (m.isDefined) s"$name: ${m.get.toString}\n" 
    else ""
  }

  private def maybeNodeHtml(name: String, m: Option[RDFNode],pm:PrefixMap) = {
    val tdstart = "<td class=" + name + ">"
    val str = if (m.isDefined) {
      val node : RDFNode = m.get
      if (node.isIRI) {
        val iri = node.toIRI
        escape(PrefixMapUtils.qualify(iri)(pm))
      } else {
        if (node.isBNode) {
          val fullStr = node.getLexicalForm
          val str = fullStr.takeRight(3)
          s"<abbr title='${fullStr}'>...$str</abbr>"
        } else
          node.getLexicalForm
      }
    }
    else ""
   val tdend = "</td>"
   tdstart + str + tdend
  }

}

object ViolationError {
  
  def msgError(msg: String): ViolationError = 
    ViolationError(msg,None,None,None,None,None,None,None)
    
  def parse(rdf:RDFReader, node: RDFNode): Try[ViolationError] = {
    ViolationErrorParser.parse(node,rdf)
  }
  
}
