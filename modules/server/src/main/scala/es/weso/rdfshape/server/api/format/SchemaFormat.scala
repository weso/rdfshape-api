package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

/** Dummy trait to differentiate schema formats from the more generic DataFormat
  * @see {@link es.weso.rdfshape.server.api.format.DataFormat}
  */
class SchemaFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType) {
  def this(format: Format) {
    this(format.name, format.mimeType)
  }
}

/** Companion object with all SchemaFormat static utilities
  */
object SchemaFormat extends FormatCompanion[SchemaFormat] {

  override lazy val availableFormats: List[SchemaFormat] =
    List(
      new SchemaFormat(Turtle),
      new SchemaFormat(JsonLd),
      new SchemaFormat(NTriples),
      new SchemaFormat(RdfXml),
      new SchemaFormat(RdfJson),
      new SchemaFormat(Trig),
      ShExC
    )
  override val defaultFormat: SchemaFormat = ShExC
}

/** Represents the mime-type "text/shex"
  */
case object ShExC
    extends SchemaFormat(
      formatName = "shexc",
      formatMimeType = new MediaType("text", "shex")
    )

/** Represents the mime-type "image/png"
  */
case class FromDataFormat(dataFormat: DataFormat)
    extends SchemaFormat(
      formatName = dataFormat.name,
      formatMimeType = dataFormat.mimeType
    )
