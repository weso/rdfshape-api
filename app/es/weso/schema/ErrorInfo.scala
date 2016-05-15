package es.weso.schema
import xml.Utility.escape

case class ErrorInfo(str: String) {
  
  def toHTML: String = {
    str
  }

}