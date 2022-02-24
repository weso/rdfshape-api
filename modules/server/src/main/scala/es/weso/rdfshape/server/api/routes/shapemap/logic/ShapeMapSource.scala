package es.weso.rdfshape.server.api.routes.shapemap.logic

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different possible ShapeMap sources sent by the client.
  * The source sent indicates the API if the shapemap was sent in raw text, as a URL
  * to be fetched or as a text file containing the shapemap.
  * In case the client submits the shapemap in several formats, the selected source will indicate the preferred one.
  */
private[api] object ShapeMapSource extends MyEnum[String] {
  type ShapeMapSource = String

  val TEXT = "byText"
  val URL  = "byUrl"
  val FILE = "byFile"

  val values                  = Set(TEXT, URL, FILE)
  val default: ShapeMapSource = TEXT
}
