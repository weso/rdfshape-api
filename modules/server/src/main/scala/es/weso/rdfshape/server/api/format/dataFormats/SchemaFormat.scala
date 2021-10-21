package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format.{Format, FormatCompanion}
import org.http4s.MediaType

/** Dummy class to differentiate shapemap formats from the more generic DataFormat
  * @see {@link DataFormat}
  */
class SchemaFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType) {
  def this(format: Format) = {
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
      formatName = "ShExC",
      formatMimeType = new MediaType("text", "shex")
    )
