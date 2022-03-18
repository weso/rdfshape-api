package es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikibase

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikibase
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikidata.WikidataObject
import es.weso.rdfshape.server.utils.networking.NetworkingUtils
import org.http4s.Uri

/** General class representing any object (entity, schema...) living in a
  * wikibase instance.
  * Unlike [[WikidataObject]], where Wikidata is the target wikibase,
  *  here a wikibase must be provided for context
  *
  * @param wikibase Wikibase where the object resides
  * @param entityUri URL where the object data can be found
  *
  * @note Currently limited to Entities/properties
  */
abstract private[api] class WikibaseObject(
    val wikibase: Wikibase,
    val entityUri: Uri
) {

  /** Either the raw contents of this object or the error
    * occurred while retrieving them
    */
  lazy val contents: Either[String, String] =
    NetworkingUtils.getUrlContents(contentUri.renderString)

  /** Short name or identifier of the entity, e.g.: Q123,
    * normally this is the last part of [[entityUri]]
    */
  val localName: String

  /** URL where the entity data can be found in raw form
    */
  val contentUri: Uri
}
