package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format.FormatCompanion
import org.http4s.MediaType

/** Dummy class to differentiate formats used for graphical representations from the more generic DataFormat
  *
  * @see {@link DataFormat}
  */
sealed class GraphicFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType) {}

/** Companion object with all RDFFormat static utilities
  */
object GraphicFormat extends FormatCompanion[GraphicFormat] {

  override lazy val availableFormats: List[GraphicFormat] =
    List(
      Svg,
      Png
    )
  override val defaultFormat: GraphicFormat = Svg
}

/** Represents the mime-type "image/svg+xml"
  */
case object Svg
    extends GraphicFormat(
      formatName = "SVG",
      formatMimeType = MediaType.image.`svg+xml`
    )

/** Represents the mime-type "image/png"
  */
case object Png
    extends GraphicFormat(
      formatName = "PNG",
      formatMimeType = MediaType.image.png
    )
