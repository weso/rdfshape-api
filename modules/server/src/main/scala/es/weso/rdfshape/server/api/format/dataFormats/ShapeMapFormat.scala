package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format.{Format, FormatCompanion}
import org.http4s.MediaType

/** Dummy trait to differentiate shapemap formats from the more generic DataFormat
  * @see {@link DataFormat}
  */
class ShapeMapFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType) {
  def this(format: Format) = {
    this(format.name, format.mimeType)
  }
}

/** Companion object with all SchemaFormat static utilities
  */
object ShapeMapFormat extends FormatCompanion[ShapeMapFormat] {

  override lazy val availableFormats: List[ShapeMapFormat] =
    List(
      Compact,
      new ShapeMapFormat(Json)
    )
  override val defaultFormat: ShapeMapFormat = Compact
}

/** Represents the mime-type "text/shex"
  */
case object Compact
    extends ShapeMapFormat(
      formatName = "compact",
      formatMimeType = new MediaType("text", "shex")
    )
