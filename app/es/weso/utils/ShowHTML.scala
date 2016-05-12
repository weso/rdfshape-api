package es.weso.utils
import scala.xml.Utility.escape

trait ShowHTML[A] {
  def toHTML(x: A): String
}

object ShowHTML {

  def code(x: String) = s"<code>${escape(x)}</code>"
  
/*  implicit val stringShowHTML = new ShowHTML[String] {
    def showHTML(s: String) = escape(s)
  }

  implicit val intShowHTML = new ShowHTML[Int] {
    def showHTML(s: Int) = s.toString
  } */

/*  implicit def seqShowHTML_UL[A](implicit sa: ShowHTML[A]) = new ShowHTML[Seq[A]] {
  def showHTML(xs: Seq[A]): String = {
    val sb = new StringBuilder
    sb.append("<ul>")
    for (x <- xs) { sb.append(s"<li>${sa.toHTML(x)}</li>") }
    sb.append("</ul>")
    sb.toString
  }
} */

/* implicit val pairShowHTML[A:ShowHTML,B:ShowHTML] = new ShowHTML[(A,B)] {
  def showHTML(x: (A,B)):String = 
    "<span class=\"pair_1\">" + 
     x._1.toHTML + 
    "</span><span class=\"pair_2\">" +
    x._2.toHTML +
    "</span>"
 } */

}


