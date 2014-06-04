package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.parser.PrefixMap
import xml.Utility.escape

case class ValidationResult(
    msg: String, 
    rs: Stream[Typing], 
    pm: PrefixMap) {

  def typing2Html(typing: Typing): String = {
    val sb = new StringBuilder
    sb.append("<ul>")
    for ((node,ts) <- typing.map) {
      sb.append("<li>" + escape(node.toString) + 
          " -> " + 
          ts.map(n => escape(n.toString).mkString(" ")))
    }
    sb.append("</ul>")
    sb.toString
  }
  
  def toHTML(cut: Int): String = {
    val sb = new StringBuilder
    sb.append("<p>" + msg + "</p>")
    sb.append("<ul>")
    for (t <- rs.take(cut)) {
      sb.append("<li>" + typing2Html(t) + "</li>")
    }
    sb.append("</ul>") 
    println("StringBuilder: " + sb.toString)
    sb.toString
  }
}

object ValidationResult {
  def empty = ValidationResult("",Stream(), PrefixMap.empty)
  
  def failure(e: Throwable) : ValidationResult = {
    ValidationResult(e.getMessage, Stream(),PrefixMap.empty)
  }

  def withMessage(msg: String) : ValidationResult = {
    ValidationResult(msg, Stream(),PrefixMap.empty)
  }
}
