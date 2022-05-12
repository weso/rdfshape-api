package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different types of objects that can be requested to
  * wikibase's API in search operations.
  *
  * @note "schema" is not a standard value for wbsearchentities, but a custom
  *       value we use in some server operations that fetch schemas
  * @see [[https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities]]
  */
private[api] object WikibaseSearchTypes extends MyEnum[String] {
  type WikibaseSearchTypes = String

  val ENTITY   = "item"
  val PROPERTY = "property"
  val LEXEME   = "lexeme"
  val FORM     = "form"
  val SENSE    = "sense"
  val SCHEMA   = "schema"

  val values: Set[WikibaseSearchTypes] =
    Set(ENTITY, PROPERTY, LEXEME, FORM, SENSE)
  val basicValues: Set[WikibaseSearchTypes] =
    Set(ENTITY, PROPERTY, LEXEME, SCHEMA)
  val default: WikibaseSearchTypes = ENTITY
}
