package es.weso.schema
import es.weso.utils.ShowHTML._

case class Explanation(str: String) {
  
  def toHTML: String = "<span class=\"explanation\">" + code(str) + "</span>"

}