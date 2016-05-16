package es.weso.schema
import es.weso.utils.ShowHTML._
import es.weso.rdf.PrefixMap

case class Explanation(str: String) {
  
  def toHTML(pm:PrefixMap): String = "<span class=\"explanation\">" + code(str) + "</span>"

}