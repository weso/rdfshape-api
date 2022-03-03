package es.weso.rdfshape.server.api.format.dataFormats.schemaFormats

import es.weso.rdfshape.server.api.format.dataFormats._
import es.weso.rdfshape.server.api.format.{Format, FormatCompanion}
import org.http4s.MediaType

/** Dummy class to differentiate shapemap formats from the more generic DataFormat
  * @see {@link DataFormat}
  */
class ShExFormat(formatName: String, formatMimeType: MediaType)
    extends SchemaFormat(formatName, formatMimeType) {
  def this(format: Format) = {
    this(format.name, format.mimeType)
  }
}

/** Companion object with all SchemaFormat static utilities
  */
object ShExFormat extends FormatCompanion[ShExFormat] {

  override lazy val availableFormats: List[ShExFormat] =
    List(
      ShExC,
      ShExJ
    )
  override val default: ShExFormat = ShExC
}

/** Represents the mime-type "text/shexc"
  */
case object ShExC
    extends ShExFormat(
      formatName = "ShExC",
      formatMimeType = new MediaType("text", "shexc")
    )

/** Represents the mime-type "text/shexj"
  */
case object ShExJ
    extends ShExFormat(
      formatName = "ShExJ",
      formatMimeType = new MediaType("text", "shexj")
    )
