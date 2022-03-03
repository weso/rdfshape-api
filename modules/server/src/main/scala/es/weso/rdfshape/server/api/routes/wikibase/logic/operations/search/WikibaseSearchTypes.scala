package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different types of objects that can be requested to
  * wikibase's API in search operations.
  *
  * @see [[https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities]]
  */
private[api] object WikibaseSearchTypes extends MyEnum[String] {
  type WikibaseSearchTypes = String

  val ENTITY   = "item"
  val PROPERTY = "property"
  val LEXEME   = "lexeme"
  val FORM     = "form"
  val SENSE    = "sense"

  val values                       = Set(ENTITY, PROPERTY, LEXEME, FORM, SENSE)
  val basicValues                  = Set(ENTITY, PROPERTY, LEXEME)
  val default: WikibaseSearchTypes = ENTITY
}
