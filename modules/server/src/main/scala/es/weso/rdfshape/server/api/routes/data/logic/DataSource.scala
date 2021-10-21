package es.weso.rdfshape.server.api.routes.data.logic

/** Enumeration of the different possible Data sources sent by the client.
  * The source sent indicates the API if the schema was sent in raw text, as a URL
  * to be fetched or as a text file containing the schema.
  * In case the client submits the data in several formats, the selected source will indicate the preferred one.
  */
private[api] object DataSource extends Enumeration {
  type DataSource = String

  val TEXT     = "#dataTextArea"
  val URL      = "#dataUrl"
  val FILE     = "#dataFile"
  val COMPOUND = "#compoundData"
  val ENDPOINT = "#dataEndpoint"

  val defaultActiveDataSource: DataSource = TEXT
}
