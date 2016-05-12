package es.weso.schema
import es.weso.utils.ShowHTML
import es.weso.utils.ShowHTML._

case class ShapeLabel(str: String) {
  def toHTML: String = "<span class=\"shape\">" + code(str) + "</span>"
}