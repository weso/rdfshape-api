package es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikibase

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikibase
import org.http4s.Uri

/** Data class representing a Wikidata entity
  */
class WikibaseSchema(
    override val wikibase: Wikibase,
    override val entityUri: Uri
) extends WikibaseObject(wikibase, entityUri) {

  override val localName: String =
    entityUri.renderString.split(":").last.stripSuffix("#")

  override val contentUri: Uri =
    wikibase.baseUrl / "wiki" / "Special:EntitySchemaText" / localName
}
