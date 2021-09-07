package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

/** Dummy class to differentiate RDF formats from the more generic DataFormat
  *
  * @see {@link es.weso.rdfshape.server.api.format.DataFormat}
  */
sealed class RDFFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType) {}

/** Companion object with all RDFFormat static utilities
  */
object RDFFormat extends FormatCompanion[RDFFormat] {

  override lazy val availableFormats: List[RDFFormat] =
    List(
      Turtle,
      NTriples,
      Trig,
      JsonLd,
      RdfXml,
      RdfJson
    )
  override val defaultFormat: RDFFormat = Turtle
}

/** Represents the mime-type "text/turtle"
  */
case object Turtle
    extends RDFFormat(
      formatName = "turtle",
      formatMimeType = new MediaType("text", "turtle")
    )

/** Represents the mime-type "application/n-triples"
  */
case object NTriples
    extends RDFFormat(
      formatName = "n-triples",
      formatMimeType = new MediaType("application", "n-triples")
    )

/** Represents the mime-type "application/trig"
  */
case object Trig
    extends RDFFormat(
      formatName = "trig",
      formatMimeType = new MediaType("application", "trig")
    )

/** Represents the mime-type "application/ld+json"
  */
case object JsonLd
    extends RDFFormat(
      formatName = "json-ld",
      formatMimeType = new MediaType("application", "ld+json")
    )

/** Represents the mime-type "application/rdf+xml"
  */
case object RdfXml
    extends RDFFormat(
      formatName = "rdf/xml",
      formatMimeType = new MediaType("application", "rdf+xml")
    )

/** Represents the mime-type "application/json"
  */
case object RdfJson
    extends RDFFormat(
      formatName = "rdf/json",
      formatMimeType = MediaType.application.json
    )
