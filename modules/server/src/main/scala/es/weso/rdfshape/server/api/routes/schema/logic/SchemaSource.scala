package es.weso.rdfshape.server.api.routes.schema.logic

/** Enumeration of the different possible Schema sources sent by the client.
  * The source sent indicates the API if the schema was sent in raw text, as a URL
  * to be fetched or as a text file containing the schema.
  * In case the client submits the schema in several formats, the selected source will indicate the preferred one.
  */
private[api] object SchemaSource extends Enumeration {
  type SchemaSource = String

  val TEXT = "byText"
  val URL  = "byUrl"
  val FILE = "byFile"

  val defaultSchemaSource: SchemaSource = TEXT
}
