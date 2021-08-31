package es.weso.rdfshape.server.api.routes.permalink.logic

import com.typesafe.scalalogging.LazyLogging

import java.net.URL
import java.util.Date

/** Data class representing a permalink
  *
  * @param longUrl      Permalink target
  * @param code         Permalink identifying code
  * @param creationDate Permalink identifying code
  */
sealed case class Permalink(
    longUrl: URL,
    code: Long,
    creationDate: Date = new Date(),
    editionDate: Date
)

private[api] object Permalink extends LazyLogging {

  /** Placeholder value used for the permalink query whenever an empty target is issued/needed.
    */
  private val emptyTargetValue = ""
}
