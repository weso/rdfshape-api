package es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikidata

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikibase.WikibaseEntity
import org.http4s.Uri

import scala.util.matching.Regex

/** Data class representing a Wikidata entity
  */
case class WikidataEntity(
    override val entityUri: Uri
) extends WikibaseEntity(Wikidata, entityUri)
    with WikidataObject {

  override val wikidataRegex: Regex =
    "(http(s)?://www.wikidata.org/entity/(.+))|(http(s)?://www.wikidata.org/wiki/(.+))".r

  checkUri()

}
