package es.weso.rdfshape.server.api.routes.endpoint.logic.query

/** Enumeration of the different possible Query sources by the client.
  * The source sent indicates the API if the Query was sent in raw text, as a URL
  * to be fetched or as a text file containing the query.
  * In case the client submits the query in several formats, the selected source will indicate the one format.
  */
private[api] object SparqlQuerySource extends Enumeration {
  type SparqlQuerySource = String

  val TEXT = "byText"
  val URL  = "byUrl"
  val FILE = "byFile"

  val defaultQuerySource: SparqlQuerySource = TEXT
}
