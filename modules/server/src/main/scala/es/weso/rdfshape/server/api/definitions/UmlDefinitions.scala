package es.weso.rdfshape.server.api.definitions

import es.weso.uml.PlantUMLOptions

/** UML-generation related data
  */
case object UmlDefinitions {

  /** Additional options passed down to PlantUML when generating diagrams on the fly.
    */
  val umlOptions: PlantUMLOptions = PlantUMLOptions(
    watermark = Some("Generated by [[https://rdfshape.weso.es rdfshape]]")
  )
}