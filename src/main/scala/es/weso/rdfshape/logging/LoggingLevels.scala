package es.weso.rdfshape.logging

/** Enumeration classifying the accepted logging levels by their String representation.
  */
case object LoggingLevels {
  type LoggingLevels = String

  val ERROR = "ERROR"
  val WARN  = "WARN"
  val INFO  = "INFO"
  val DEBUG = "DEBUG"
  val TRACE = "TRACE"
}
