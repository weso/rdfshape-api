package es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikidata

import org.http4s.Uri

import scala.util.matching.Regex

/** Trait defining several utilities and guidelines for wikibase items
  * residing in Wikidata
  */
trait WikidataObject {

  /** Uri unique to the wikidata object being identified
    */
  val entityUri: Uri

  /** URL where the entity data can be found in raw form
    */
  val contentUri: Uri

  /** Regular expression used to recognize wikidata objects of the required type
    * When using Wikidata, we know the format of the URIs we expect, so we can
    * define a regex to notice if the input URI is a valid Wikidata item or not
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
