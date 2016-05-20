package es.weso.schema
import es.weso.rdf.PrefixMap

case class Result(
    isValid: Boolean,
    message: String,
    solutions: Seq[Solution],
    errors: Seq[ErrorInfo]) {
  
  def toHTML(cut: Int = 1, schema:Schema): String = {
    val sb = new StringBuilder
    val pm = schema.pm
    if (isValid) {
     for ((solution, n) <- solutions zip (1 to cut)) {
      sb ++= "<h2 class='result'>Result" + printNumber(n, cut) + "</h2>"
      sb ++= schema.beforeSolution
      sb ++= solution.toHTML(pm)
     }
    } else {
    sb++="<div class=\"errors\">"
    sb ++= "<table class='display' id='results' >"
    sb ++= schema.beforeErrors
    for (error <- errors) {
      sb ++= error.toHTML(pm)
     }
    sb ++= schema.afterErrors
    sb++="</table>"
    }
    sb.toString
  }
  
   def printNumber(n: Int, cut: Int): String = {
    if (n == 1 && cut == 1) ""
    else n.toString
  }

}

object Result {
  def empty = Result(isValid = true, message = "", solutions = Seq(), errors=Seq()) 
  
  def errStr(str: String) = Result(isValid = false, message = str, solutions = Seq(), errors = Seq())
  
}