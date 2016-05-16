package es.weso.schema

import xml.Utility.escape
import es.weso.rdf.PrefixMap

case class ErrorInfo(str: String) {
  
  def toHTML(pm: PrefixMap): String = {
    str
  }

}