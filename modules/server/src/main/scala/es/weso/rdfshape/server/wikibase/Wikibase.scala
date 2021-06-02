package es.weso.rdfshape.server.wikibase
import es.weso.rdf.nodes._
import org.http4s._
import org.http4s.implicits._

abstract class Wikibase {
  def name: String
  def endpointUrl: IRI
  def schemaEntityUri(str: String): Uri
}

case object Wikidata extends Wikibase {
  def name             = "wikidata"
  def url              = uri"https://www.wikidata.org"
  def endpointUrl: IRI = IRI("https://query.wikidata.org/sparql")

  def schemaEntityUri(wdSchema: String): Uri = {
    val uri = uri"https://www.wikidata.org".withPath(
      Uri.Path.unsafeFromString(s"/wiki/Special:EntitySchemaText/${wdSchema}")
    )
    uri
  }

}
