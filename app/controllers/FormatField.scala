package controllers

import anorm._

case class FormatField(id: Long, iriName : String, label: String, langName : String, format: String,votes: Long)