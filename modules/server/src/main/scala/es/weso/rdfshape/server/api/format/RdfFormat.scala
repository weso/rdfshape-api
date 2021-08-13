package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

/** Dummy trait to differentiate RDF formats from the more generic DataFormat
  * @see {@link es.weso.rdfshape.server.api.format.DataFormat}
  */
sealed trait RDFFormat extends DataFormat

/** Represents the mime-type "text/turtle"
  */
case object Turtle extends RDFFormat {
  override val name     = "turtle"
  override val mimeType = new MediaType("text", "turtle")
}

/** Represents the mime-type "application/n-triples"
  */
case object NTriples extends RDFFormat {
  override val name     = "n-triples"
  override val mimeType = new MediaType("application", "n-triples")
}

/** Represents the mime-type "application/trig"
  */
case object Trig extends RDFFormat {
  override val name     = "trig"
  override val mimeType = new MediaType("application", "trig")
}

/** Represents the mime-type "application/ld+json"
  */
case object JsonLd extends RDFFormat {
  override val name     = "json-ld"
  override val mimeType = new MediaType("application", "ld+json")
}

/** Represents the mime-type "application/rdf+xml"
  */
case object RdfXml extends RDFFormat {
  override val name     = "rdf/xml"
  override val mimeType = new MediaType("application", "rdf+xml")
}

/** Represents the mime-type "application/json"
  */
case object RdfJson extends RDFFormat {
  override val name                = "rdf/json"
  override val mimeType: MediaType = MediaType.application.json
}
