package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikidata

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikibase.WikibaseProperty
import org.http4s.Uri

import scala.util.matching.Regex

/** Data class representing a Wikidata schema
  */
case class WikidataProperty(
    override val entityUri: Uri
) extends WikibaseProperty(Wikidata, entityUri)
    with WikidataObject {

  override val wikidataRegex: Regex =
    "http(s)?://www.wikidata.org/wiki/Property:(.+)".r

  checkUri()
}
