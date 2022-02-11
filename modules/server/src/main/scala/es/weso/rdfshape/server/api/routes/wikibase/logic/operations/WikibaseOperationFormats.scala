package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

/** Enumeration of the different formats that can be requested to wikibase's API.
  * The "fm" formats are meant for debugging
  *
  * @see [[https://www.mediawiki.org/wiki/Wikibase/API#Request_Format]]
  */
private[api] object WikibaseOperationFormats extends Enumeration {
  type WikibaseQueryFormat = String

  val JSON    = "json"
  val JSON_FM = "jsonfm"
  val XML     = "xml"
  val XML_FM  = "xmlfm"

  val default: WikibaseQueryFormat = JSON
}
