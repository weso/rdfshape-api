package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format.FormatCompanion
import org.http4s.MediaType

/** Dummy class to differentiate HTML-based formats from the more generic DataFormat
  *
  * @see {@link DataFormat}
  */
class HtmlFormat(formatName: String)
    extends DataFormat(formatName, MediaType.text.html) {}

/** Companion object with all HtmlFormat static utilities
  */
object HtmlFormat extends FormatCompanion[HtmlFormat] {

  override lazy val availableFormats: List[HtmlFormat] =
    List(
      HtmlRdfa11,
      HtmlMicrodata
    )
  override val default: HtmlFormat = HtmlRdfa11
}

/** Represents the mime-type "text/html" when used along rdfa11
  */
case object HtmlRdfa11 extends HtmlFormat(formatName = "html-rdfa11")

/** Represents the mime-type "text/html" when used along microdata
  */
case object HtmlMicrodata extends HtmlFormat(formatName = "html-microdata")
