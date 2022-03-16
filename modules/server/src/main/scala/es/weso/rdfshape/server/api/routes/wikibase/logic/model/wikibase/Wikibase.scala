package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase

import es.weso.rdfshape.server.implicits.codecs.encodeUri
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax

/** Abstract representation of a wikibase instance
  *
  * @param name     Given name of the wikibase instance
  * @param baseUrl  Base URL where the instance is deployed (e.g. [[https://www.wikidata.org/]])
  * @param queryUrl SPARQL query endpoint of the wikibase instance, where SPARQL queries are targeted
  *                 It may vary depending on the instance.
  */
private[api] case class Wikibase(
    name: Option[String] = Option("wikibase instance"),
    baseUrl: Uri = uri"",
    queryUrl: Uri = uri""
) {

  /** Base API endpoint of the wikibase instance (e.g. [[https://www.wikidata.org/w/api.php]])
    *
    * @note Unlike [[queryUrl]], this can be inferred from [[baseUrl]]
    */
  lazy val apiUrl: Uri = baseUrl / "w" / "api.php"

  /** Given a schema identifier, return it's location inside the wikibase instance
    * Default implementation is based on Wikidata's and should be overridden.
    *
    * @param schema String representation of the schema identifier
    * @return Uri where the schema can be accessed
    */
  def schemaEntityUri(schema: String): Uri =
    baseUrl / "wiki" / "Special:EntitySchemaText" / schema

  /** Return whether if tow wikibase instances are the same or not
    *
    * @param other Other item being compared
    * @return True if the wikibase instances share a base URL, false otherwise
    */
  override def equals(other: Any): Boolean = {
    other match {
      case wb: Wikibase => baseUrl == wb.baseUrl
      case _            => false
    }
  }
}

object Wikibase {

  /** JSON encoder for [[Wikibase]]
    */
  implicit val encode: Encoder[Wikibase] = (wikibase: Wikibase) =>
    Json.fromFields(
      List(
        ("name", wikibase.name.asJson),
        ("baseUrl", wikibase.baseUrl.asJson),
        ("queryUrl", wikibase.queryUrl.asJson),
        ("apiUrl", wikibase.apiUrl.asJson)
      )
    )
}
