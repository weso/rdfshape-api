package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikidata

import org.http4s.Uri
import org.http4s.implicits.http4sLiteralsSyntax

import scala.util.matching.Regex

/** Data class representing a Wikidata entity
  */
case class WikidataEntity(
    override val entityUri: Uri
) extends WikidataObject(entityUri) {

  override val wikidataRegex: Regex =
    "(http(s)?://www.wikidata.org/entity/(.+))|(http(s)?://www.wikidata.org/wiki/(.+))".r

  checkUri()

  override val localName: String =
    entityUri.renderString.split("/").last.stripSuffix("#")

  override val contentUri: Uri =
    uri"https://www.wikidata.org" / "wiki" / "Special:EntityData" / (localName + ".ttl")
}
