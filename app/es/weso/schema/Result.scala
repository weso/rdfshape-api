package es.weso.schema

case class Result(
    isValid: Boolean,
    message: String,
    solutions: Seq[Solution],
    errors: Seq[ErrorInfo]) {
  
  def toHTML(cut: Int = 1): String = {
    val sb = new StringBuilder
    if (isValid) {
     for ((solution, n) <- solutions zip (1 to cut)) {
      sb ++= "<h2 class='result'>Result" + printNumber(n, cut) + "</h2>"
      sb ++= solution.toHTML
     }
    } else {
    sb++="<div class=\"errors\">"
    for (error <- errors) {
      sb ++= "<table class='error'>"
      sb ++= error.toHTML
     }
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
}