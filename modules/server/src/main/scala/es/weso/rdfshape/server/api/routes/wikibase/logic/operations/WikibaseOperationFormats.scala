package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different formats that can be requested to wikibase's API.
  * The "fm" formats are meant for debugging
  *
  * @see [[https://www.mediawiki.org/wiki/Wikibase/API#Request_Format]]
  */
private[api] object WikibaseOperationFormats extends MyEnum[String] {
  type WikibaseOperationFormats = String

  val JSON    = "json"
  val JSON_FM = "jsonfm"
  val XML     = "xml"
  val XML_FM  = "xmlfm"

  val values                            = Set(JSON, JSON_FM, XML, XML_FM)
  val default: WikibaseOperationFormats = JSON
}
