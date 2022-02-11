package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get

/** Enumeration of the different properties of objects that can be requested to
  * wikibase's API in get operations.
  *
  * @see [[https://www.wikidata.org/w/api.php?action=help&modules=wbgetentitiess]]
  */
private[api] object WikibasePropTypes extends Enumeration {
  type WikibasePropTypes = String

  val ALIASES        = "aliases"
  val CLAIMS         = "claims"
  val DATATYPE       = "datatype"
  val DESCRIPTIONS   = "descriptions"
  val INFO           = "info"
  val LABELS         = "labels"
  val SITELINKS      = "sitelinks"
  val SITELINKS_URLS = "sitelinks/urls"

  /** Default list of props request in get operations, mirroring the default used
    * by Mediawiki's API
    */
  val default: List[WikibasePropTypes] =
    List(INFO, SITELINKS, ALIASES, LABELS, DESCRIPTIONS, CLAIMS, DATATYPE)
}
