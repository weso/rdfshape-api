package es.weso.rdfshape.server.api.format.dataFormats.schemaFormats

import es.weso.rdfshape.server.api.format.dataFormats._
import es.weso.rdfshape.server.api.format.{Format, FormatCompanion}
import org.http4s.MediaType

/** Dummy class to differentiate shapemap formats from the more generic DataFormat
  *
  * @see {@link DataFormat}
  */
class ShaclFormat(formatName: String, formatMimeType: MediaType)
    extends SchemaFormat(formatName, formatMimeType) {
  def this(format: Format) = {
    this(format.name, format.mimeType)
  }
}

/** Companion object with all SchemaFormat static utilities
  */
object ShaclFormat extends FormatCompanion[ShaclFormat] {

  override lazy val availableFormats: List[ShaclFormat] =
    List(
      new ShaclFormat(Turtle),
      new ShaclFormat(NTriples),
      new ShaclFormat(NQuads),
      new ShaclFormat(Trig),
      new ShaclFormat(JsonLd),
      new ShaclFormat(RdfXml)
    )
  override val defaultFormat: ShaclFormat = new ShaclFormat(Turtle)
}
