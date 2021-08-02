package es.weso.rdfshape.server.wikibase

import es.weso.rdf.nodes._
import org.http4s._
import org.http4s.implicits._

/** Abstract representation of a wikibase instance
  *
  * @param name        Given name of the wikibase instance
  * @param baseUrl     Base URL where the instance is deployed (e.g. [[https://www.wikidata.org/]])
  * @param endpointUrl API endpoint of the wikibase instance, where queries are usually made
  */
abstract sealed class Wikibase(
    val name: String,
    val baseUrl: Uri,
    val endpointUrl: IRI
) {

  /** Given a schema identifier, return it's location inside the wikibase instance
    *
    * @param schema String representation of the schema identifier
    * @return Uri where the schema can be accessed
    */
  def schemaEntityUri(schema: String): Uri
}

/** A sub-instance of the more general Wikibase class, containing the data required to access [[https://www.wikidata.org/ Wikidata]]
  *
  * @see {@link es.weso.rdfshape.server.wikibase.Wikibase}
  */
case object Wikidata
    extends Wikibase(
      name = "wikidata",
      baseUrl = uri"https://www.wikidata.org",
      endpointUrl = IRI("https://query.wikidata.org/sparql")
    ) {

  def schemaEntityUri(schema: String): Uri = {
    val uri = baseUrl.withPath(
      Uri.Path.unsafeFromString(s"/wiki/Special:EntitySchemaText/$schema")
    )
    uri
  }
}
