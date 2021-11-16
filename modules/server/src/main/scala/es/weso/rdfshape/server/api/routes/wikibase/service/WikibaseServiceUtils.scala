package es.weso.rdfshape.server.api.routes.wikibase.service

import cats.implicits._
import io.circe.Json
import io.circe.parser.parse

private[service] class WikibaseServiceUtils {}

/** Static utilities used by the Wikibase service
  */
//noinspection HttpUrlsUsage,SpellCheckingInspection
object WikibaseServiceUtils {

  /** For a given entity, create the JSON structure accepted by the Shexer API
    *
    * @param entity Entity to be examined by Shexer
    * @return JSON object with the structure accepted by the Shexer API, adapted for the given entity
    */
  def mkShexerParams(entity: String): Either[String, Json] = for {
    prefixes <- wikidataPrefixes
  } yield Json.fromFields(
    List(
      ("prefixes", prefixes),
      (
        "shape_map",
        Json.fromString(
          "SPARQL'SELECT DISTINCT ?virus WHERE {   VALUES ?virus {  wd:Q82069695  }  }'@<Virus>  "
        )
      ),
      ("endpoint", Json.fromString("https://query.wikidata.org/sparql")),
      ("all_classes", Json.False),
      ("query_depth", Json.fromInt(1)),
      ("threshold", Json.fromInt(0)),
      (
        "instantiation_prop",
        Json.fromString("http://www.wikidata.org/prop/direct/P31")
      ),
      ("disable_comments", Json.True),
      ("shape_qualifiers_mode", Json.True),
      (
        "namespaces_for_qualifiers",
        Json.arr(Json.fromString("http://www.wikidata.org/prop/"))
      )
    )
  )

  /** @return JSON containing all the prefixed used by Wikidata
    */
  def wikidataPrefixes: Either[String, Json] = {
    val json =
      """{
    "http://wikiba.se/ontology#": "wikibase",
    "http://www.bigdata.com/rdf#": "bd",
    "http://www.wikidata.org/entity/": "wd",
    "http://www.wikidata.org/prop/direct/": "wdt",
    "http://www.wikidata.org/prop/direct-normalized/": "wdtn",
    "http://www.wikidata.org/entity/statement/": "wds",
    "http://www.wikidata.org/prop/": "p",
    "http://www.wikidata.org/reference/": "wdref",
    "http://www.wikidata.org/value/": "wdv",
    "http://www.wikidata.org/prop/statement/": "ps",
    "http://www.wikidata.org/prop/statement/value/": "psv",
    "http://www.wikidata.org/prop/statement/value-normalized/": "psn",
    "http://www.wikidata.org/prop/qualifier/": "pq",
    "http://www.wikidata.org/prop/qualifier/value/": "pqv",
    "http://www.wikidata.org/prop/qualifier/value-normalized/": "pqn",
    "http://www.wikidata.org/prop/reference/": "pr",
    "http://www.wikidata.org/prop/reference/value/": "prv",
    "http://www.wikidata.org/prop/reference/value-normalized/": "prn",
    "http://www.wikidata.org/prop/novalue/": "wdno"
   }"""
    parse(json).leftMap(e => s"Error parsing prefixes: $e")
  }

  /** Convert the response from Wikibase "wbcontentlanguages" to a more convenient JSON structure
    * @param json Input JSON, as received from Wikibase
    * @return Either a JSON representation of the languages in the Wikibase, or an error message
    */
  def convertLanguages(json: Json): Either[String, Json] = for {
    languagesObj <- json.hcursor
      .downField("query")
      .downField("wbcontentlanguages")
      .focus
      .toRight(s"Error obtaining query/wbcontentlanguages at ${json.spaces2}")
    keys <- languagesObj.hcursor.keys.toRight(
      s"Error obtaining values from languages: ${languagesObj.spaces2}"
    )
    converted = Json.fromValues(
      keys.map(key =>
        Json.fromFields(
          List(
            (
              "label",
              languagesObj.hcursor
                .downField(key)
                .downField("code")
                .focus
                .getOrElse(Json.Null)
            ),
            (
              "name",
              languagesObj.hcursor
                .downField(key)
                .downField("autonym")
                .focus
                .getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield {
    converted
  }

  /** Convert the response from Wikibase "wbsearchentities" to a more convenient JSON structure
    * @param json Input JSON, as received from Wikibase
    * @return Either a JSON representation of the entities in the Wikibase, or an error message
    */
  def convertEntities(json: Json): Either[String, Json] = for {
    entities <- json.hcursor
      .downField("search")
      .values
      .toRight("Error obtaining search value")
    converted = Json.fromValues(
      entities.map((value: Json) =>
        Json.fromFields(
          List(
            (
              "label",
              value.hcursor.downField("label").focus.getOrElse(Json.Null)
            ),
            ("id", value.hcursor.downField("id").focus.getOrElse(Json.Null)),
            (
              "uri",
              value.hcursor.downField("concepturi").focus.getOrElse(Json.Null)
            ),
            (
              "descr",
              value.hcursor.downField("description").focus.getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield converted
}
