package es.weso.schema
import es.weso.rdf.nodes._
import scala.xml.Utility.escape

case class Solution(map: Map[RDFNode,InfoNode]) {
  
  def toHTML: String = {
    val sb = new StringBuilder
    sb ++= """<table class=\"result\"><tr><th>Node</th><th>Shapes</th></tr>"""
    for (pair <- map.toSeq) {
      val (node,info) = pair
      sb ++= ("<tr><td class=\"node\">" + node2Html(node) + "</td>" +
              "<td class=\"shapes\">" + info.toHTML + "</td></tr>")
    }
    sb ++= "</table>"
    sb.toString
  }
  
   def node2Html(node: RDFNode): String = {
    if (node.isIRI) code(node.toIRI.toString)
    else code(node.toString)
  }

  def code(str: String): String = {
    s"<code>${escape(str)}</code>"
  }
   
  
}
