package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

/** Enumeration of the different types of objects that can be requested to
  * wikibase's API in search operations.
  *
  * @see [[https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities]]
  */
private[api] object WikibaseSearchTypes extends Enumeration {
  type WikibaseSearchTypes = String

  val ITEM     = "item"
  val PROPERTY = "property"
  val LEXEME   = "lexeme"
  val FORM     = "form"
  val SENSE    = "sense"

  val default: WikibaseSearchTypes = ITEM
}
