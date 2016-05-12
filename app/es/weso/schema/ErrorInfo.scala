package es.weso.schema
import es.weso.utils.ShowHTML
import es.weso.utils.ShowHTML._

case class ErrorInfo(str: String) {
  
  implicit val eShowHTML = new ShowHTML[ErrorInfo] {
    def toHTML(e: ErrorInfo) = code(e.str)
  }

}