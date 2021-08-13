package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

/** Dummy trait to differentiate HTML-based formats from the more generic DataFormat
  * @see {@link es.weso.rdfshape.server.api.format.DataFormat}
  */
sealed trait HtmlFormat extends DataFormat

/** Represents the mime-type "text/html" when used along rdfa11
  */
case object HtmlRdfa11 extends HtmlFormat {
  override val name                = "html-rdfa11"
  override val mimeType: MediaType = MediaType.text.html
}

/** Represents the mime-type "text/html" when used along microdata
  */
case object HtmlMicrodata extends HtmlFormat {
  override val name                = "html-microdata"
  override val mimeType: MediaType = MediaType.text.html
}
