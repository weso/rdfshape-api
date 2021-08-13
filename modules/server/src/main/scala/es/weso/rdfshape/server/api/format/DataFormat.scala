package es.weso.rdfshape.server.api.format

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
    Turtle,
    JsonLd,
    NTriples,
    RdfXml,
    RdfJson,
    Trig,
    HtmlMicrodata,
    HtmlRdfa11,
    Dot,
    Svg,
    Png,
    JsonDataFormat
  )
  override val defaultFormat: DataFormat = Turtle
}

/** Represents the mime-type "application/json"
  */
case object JsonDataFormat
    extends DataFormat(
      formatName = "json",
      formatMimeType = new MediaType("application", "json")
    )

/** Represents the mime-type "text/vnd.graphviz"
  */
case object Dot
    extends DataFormat(
      formatName = "dot",
      formatMimeType = new MediaType("text", "vnd.graphviz")
    )

/** Represents the mime-type "image/svg+xml"
  */
case object Svg
    extends DataFormat(
      formatName = "svg",
      formatMimeType = MediaType.image.`svg+xml`
    )

/** Represents the mime-type "image/png"
  */
case object Png
    extends DataFormat(
      formatName = "png",
      formatMimeType = MediaType.image.png
    )
