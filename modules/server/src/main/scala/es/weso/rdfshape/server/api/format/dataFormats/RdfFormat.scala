package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format.FormatCompanion
import es.weso.rdfshape.server.api.routes.data.logic.types.merged.DataCompound
import org.http4s.MediaType

/** Dummy class to differentiate RDF formats from the more generic DataFormat
  *
  * @see {@link DataFormat}
  */
sealed class RdfFormat(formatName: String, formatMimeType: MediaType)
    extends DataFormat(formatName, formatMimeType)

/** Companion object with all RDFFormat static utilities
  */
object RdfFormat extends FormatCompanion[RdfFormat] {

  override lazy val availableFormats: List[RdfFormat] =
    List(
      Turtle,
      NTriples,
      NQuads,
      Trig,
      JsonLd,
      RdfXml,
      RdfJson,
      Mixed
    )
  override val default: RdfFormat = Turtle
}

/** Represents the mime-type "text/turtle"
  */
case object Turtle
    extends RdfFormat(
      formatName = "Turtle",
      formatMimeType = new MediaType("text", "turtle")
    )

/** Represents the mime-type "application/n-triples"
  */
case object NTriples
    extends RdfFormat(
      formatName = "N-Triples",
      formatMimeType = new MediaType("application", "n-triples")
    )

/** Represents the mime-type "application/n-quads"
  */
case object NQuads
    extends RdfFormat(
      formatName = "N-Quads",
      formatMimeType = new MediaType("application", "n-quads")
    )

/** Represents the mime-type "application/trig"
  */
case object Trig
    extends RdfFormat(
      formatName = "TriG",
      formatMimeType = new MediaType("application", "trig")
    )

/** Represents the mime-type "application/ld+json"
  */
case object JsonLd
    extends RdfFormat(
      formatName = "JSON-LD",
      formatMimeType = new MediaType("application", "ld+json")
    )

/** Represents the mime-type "application/rdf+xml"
  */
case object RdfXml
    extends RdfFormat(
      formatName = "RDF/XML",
      formatMimeType = new MediaType("application", "rdf+xml")
    )

/** Represents the mime-type "application/json"
  */
case object RdfJson
    extends RdfFormat(
      formatName = "RDF/JSON",
      formatMimeType = MediaType.application.json
    )

/** Fictional format used in [[DataCompound]] instances
  */
case object Mixed
    extends RdfFormat(
      formatName = "mixed",
      formatMimeType = new MediaType("application", "mixed")
    )
