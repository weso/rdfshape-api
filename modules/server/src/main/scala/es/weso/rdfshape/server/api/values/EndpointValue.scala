package es.weso.rdfshape.server.api.values

/** Data class representing any endpoint from where information is fetched or that identifies RDF data
  * @param endpoint Base endpoint
  * @param node Specific information node
  * TODO
  */
case class EndpointValue(endpoint: Option[String], node: Option[String])
