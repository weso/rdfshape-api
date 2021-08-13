package es.weso.rdfshape.server.api.values

/** Data class representing a Wikidata entity
  *
  * @param entity Entity of which the data is contained
  */
case class WikidataEntityValue(entity: Option[String])
