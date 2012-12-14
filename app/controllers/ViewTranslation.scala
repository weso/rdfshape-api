package controllers
import anorm._

case class ViewTranslation(id: Long, iri: String,langCode:String,label:String,votes:Long = 1)
