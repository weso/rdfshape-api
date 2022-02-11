package es.weso.rdfshape.server.utils.error.exceptions

import scala.util.control.NoStackTrace

/** Custom exception thrown when a failure occurs while converting JSON data
  *
  * @param message Reason/explanation of why the exception occurred
  */
final case class JsonConversionException(
    private val message: String
) extends RuntimeException(message)
    with NoStackTrace
