package es.weso.rdfshape.server.api.format.dataFormats.schemaFormats

import es.weso.rdfshape.server.api.format.dataFormats._
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

  override lazy val availableFormats: List[SchemaFormat] = {
    (ShExFormat.availableFormats ++
      ShaclFormat.availableFormats).distinct
  }
  override val defaultFormat: SchemaFormat = ShExC
}
