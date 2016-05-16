package es.weso.schema
import es.weso.utils.ShowHTML
import es.weso.utils.ShowHTML._
import es.weso.rdf.PrefixMap

case class ShapeLabel(str: String) {
  def toHTML(pm: PrefixMap): String = "<span class=\"shape\">" + code(str) + "</span>"
}