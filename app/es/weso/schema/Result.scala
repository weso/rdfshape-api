package es.weso.schema

case class Result(
    isValid: Boolean,
    message: String,
    solutions: Seq[Solution],
    errors: Seq[ErrorInfo]) {
  
  def toHTML(cut: Int = 1): String = {
    val sb = new StringBuilder
    for ((solution, n) <- solutions zip (1 to cut)) {
      sb.append("<h2 class='result'>Result" + printNumber(n, cut) + "</h2>")
      sb.append(solution.toHTML)
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