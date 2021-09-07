package es.weso.rdfshape.server.api.routes.wikibase.logic

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.http4s.Uri
import org.http4s.implicits._

import scala.util.matching.Regex

/** Data class containing the information to fetch a given WikibaseEntity
  */
private[wikibase] case class WikibaseEntity(
    localName: String,
    uri: Uri,
    sourceUri: String
)

/** Static utilities to aid in converting information to WikibaseEntity instances
  */
object WikibaseEntity extends LazyLogging {

  /** Create a WikibaseEntity instance from a wikidata URI
    *
    * @param entity String containing an entity unique URI in wikidata (e.g.: https://www.wikidata.org/wiki/Q14317)
    * @return Either an instance of {@link es.weso.rdfshape.server.api.routes.wikibase.logic.WikibaseEntity} containing the entity information, or an error message
    */
  def uriToEntity(entity: String): Either[String, WikibaseEntity] = {
    val wdRegex = "http://www.wikidata.org/entity/(.*)".r
    entity match {
      case wdRegex(localName) =>
        val uri =
          uri"https://www.wikidata.org" / "wiki" / "Special:EntityData" / (localName + ".ttl")
        WikibaseEntity(localName, uri, entity).asRight[String]
      case _ =>
        s"Entity: $entity doesn't match regular expression: $wdRegex"
          .asLeft[WikibaseEntity]
    }
  }

  /** Create a WikibaseEntity instance from a wikidata URI (alternate)
    *
    * @param entity String containing an entity unique URI in wikidata (e.g.: https://www.wikidata.org/wiki/Q14317)
    * @return Either an instance of {@link es.weso.rdfshape.server.api.routes.wikibase.logic.WikibaseEntity} containing the entity information, or an error message
    */
  def uriToEntity2(entity: String): Either[String, WikibaseEntity] = {
    val wdRegex: Regex = "<(http://www.wikidata.org/entity/(.*))>".r
    entity match {
      case wdRegex(_, _) =>
        val matches = wdRegex.findAllIn(entity)
        logger.debug(s"Wikidata matches: ${matches.groupCount}")
        if(matches.groupCount == 2) {
          val localName = matches.group(2)
          val sourceUri = matches.group(1)
          val uri =
            uri"https://www.wikidata.org" / "wiki" / "Special:EntityData" / (localName + ".ttl")
          logger.debug(s"Wikidata item uri: $uri")
          WikibaseEntity(localName, uri, sourceUri).asRight[String]
        } else
          s"Entity: $entity doesn't match regular expression: $wdRegex"
            .asLeft[WikibaseEntity]
      case _ =>
        s"Entity: $entity doesn't match regular expression: $wdRegex"
          .asLeft[WikibaseEntity]
    }
  }
}
