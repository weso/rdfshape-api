package es.weso.rdfshape.server.api.routes.wikibase.logic.model

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import org.http4s.implicits.http4sLiteralsSyntax

/** A sub-instance of the more general [[Wikibase]] class,
  * configured to reference and access Wikidata
  *
  * @see [[https://www.wikidata.org/ Wikidata]]
  */
private[api] object Wikidata
    extends Wikibase(
      name = Option("wikidata"),
      baseUrl = uri"https://www.wikidata.org",
      queryUrl = uri"https://query.wikidata.org" / "sparql"
    ) {

  /** @return List of tuples with all the prefixes used by Wikidata
    *          an their short-key values
    */
  lazy val wikidataPrefixes: List[(String, String)] = {
    //noinspection HttpUrlsUsage,SpellCheckingInspection

    List(
      ("wikibase", "http://wikiba.se/ontology#"),
      ("bd", "http://www.bigdata.com/rdf#"),
      ("wd", "http://www.wikidata.org/entity/"),
      ("wdt", "http://www.wikidata.org/prop/direct/"),
      ("wdtn", "http://www.wikidata.org/prop/direct-normalized/"),
      ("wds", "http://www.wikidata.org/entity/statement/"),
      ("p", "http://www.wikidata.org/prop/"),
      ("wdref", "http://www.wikidata.org/reference/"),
      ("wdv", "http://www.wikidata.org/value/"),
      ("ps", "http://www.wikidata.org/prop/statement/"),
      ("psv", "http://www.wikidata.org/prop/statement/value/"),
      ("psn", "http://www.wikidata.org/prop/statement/value-normalized/"),
      ("pq", "http://www.wikidata.org/prop/qualifier/"),
      ("pqv", "http://www.wikidata.org/prop/qualifier/value/"),
      ("pqn", "http://www.wikidata.org/prop/qualifier/value-normalized/"),
      ("pr", "http://www.wikidata.org/prop/reference/"),
      ("prv", "http://www.wikidata.org/prop/reference/value/"),
      ("prn", "http://www.wikidata.org/prop/reference/value-normalized/"),
      ("wdno", "http://www.wikidata.org/prop/novalue/")
    )
  }

  /** [[PrefixMap]] instance containing all Wikidata prefixes in [[wikidataPrefixes]]
    */
  lazy val wikidataPrefixMap: PrefixMap = {
    PrefixMap.fromMap(wikidataPrefixes.toMap.view.mapValues(IRI.apply).toMap)
  }
}
