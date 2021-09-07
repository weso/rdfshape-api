package es.weso.rdfshape.server.utils.error.exceptions

import scala.util.control.NoStackTrace

/** Custom exception thrown when a failure occurs when operating on Wikibase data
  *
  * @param message Reason/explanation of why the exception occurred
  */
final case class WikibaseServiceException(
    private val message: String
) extends RuntimeException(message)
    with NoStackTrace
