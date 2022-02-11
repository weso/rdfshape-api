package es.weso.rdfshape.server.html2rdf

/** Enum listing the accepted sources from which RDF may be extracted.
  */
case object RdfSourceTypes extends Enumeration {
  val STRING: RdfSourceTypes.Value = Value("String")
  val URI: RdfSourceTypes.Value    = Value("URI")
}
