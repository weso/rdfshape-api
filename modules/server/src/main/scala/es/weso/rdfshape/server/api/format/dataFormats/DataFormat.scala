package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format._
import org.http4s.MediaType

/** Extension of the Format interface to represent RDF data formats
  */
class DataFormat(formatName: String, formatMimeType: MediaType) extends Format {
  override val name: String        = formatName
  override val mimeType: MediaType = formatMimeType
}

/** Companion object with all DataFormat static utilities
  */
object DataFormat extends FormatCompanion[DataFormat] {

  override lazy val availableFormats: List[DataFormat] = List(
    Json,
    Dot,
    Svg,
    Png,
    Turtle,
    NTriples,
    Trig,
    JsonLd,
    RdfXml,
    RdfJson,
    HtmlMicrodata,
    HtmlRdfa11,
    ShExC,
    Compact
  )
  override val defaultFormat: DataFormat = Json
}

/** Represents the mime-type "application/json"
  */
case object Json
    extends DataFormat(
      formatName = "JSON",
      formatMimeType = new MediaType("application", "json")
    )

/** Represents the mime-type "text/vnd.graphviz", used by graphviz
  */
case object Dot
    extends DataFormat(
      formatName = "DOT",
      formatMimeType = new MediaType("text", "vnd.graphviz")
    )
