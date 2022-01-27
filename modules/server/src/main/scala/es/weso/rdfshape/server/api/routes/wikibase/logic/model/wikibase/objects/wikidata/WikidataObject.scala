package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikidata

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.WikibaseObject
import es.weso.rdfshape.server.utils.networking.NetworkingUtils
import org.http4s.Uri

import scala.util.matching.Regex

/** Abstract class representing any object (entity, schema...) living in
  * Wikidata
  */
abstract class WikidataObject(
    override val entityUri: Uri
) extends WikibaseObject(entityUri) {

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

  /** Regular expression used to recognize wikidata objects of the required type
    */
  val wikidataRegex: Regex

  /** Assert the given [[entityUri]] is a valid uri complying with
    * [[wikidataRegex]]
    */
  def checkUri(): Unit = assume(
    wikidataRegex.matches(entityUri.renderString),
    s"Uri '${entityUri.renderString}' does not comply with '${wikidataRegex.regex}'"
  )
}
