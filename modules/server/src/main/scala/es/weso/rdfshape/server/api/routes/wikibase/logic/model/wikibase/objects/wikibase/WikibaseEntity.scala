package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikibase

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikibase
import org.http4s.Uri

/** Data class representing a Wikidata entity
  */
class WikibaseEntity(
    override val wikibase: Wikibase,
    override val entityUri: Uri
) extends WikibaseObject(wikibase, entityUri) {

  override val localName: String =
    entityUri.renderString.split("/").last.stripSuffix("#")

  override val contentUri: Uri =
    wikibase.baseUrl / "wiki" / "Special:EntityData" / (localName + ".ttl")
}
