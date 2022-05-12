package es.weso.rdfshape.server.api.routes.data.logic

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different possible Data sources sent by the client.
  * The source sent indicates the API if the schema was sent in raw text, as a URL
  * to be fetched or as a text file containing the data.
  * In case the client submits the data in several formats, the selected source will indicate the preferred one.
  */
private[api] object DataSource extends MyEnum[String] {
  type DataSource = String
  val TEXT     = "byText"
  val URL      = "byUrl"
  val FILE     = "byFile"
  val COMPOUND = "byCompound"

  val values: Set[DataSource] =
    Set(TEXT, URL, FILE, COMPOUND)
  val default: DataSource = TEXT
}
