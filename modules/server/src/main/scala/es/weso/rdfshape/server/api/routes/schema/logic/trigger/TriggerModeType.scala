package es.weso.rdfshape.server.api.routes.schema.logic.trigger

/** Enumeration of the different possible Validation Triggers sent by the client.
  * The trigger sent indicates the API how to proceed with validations
  */
private[schema] object TriggerModeType extends Enumeration {
  type TriggerModeType = String

  val SHAPEMAP            = "shapeMap"
  val TARGET_DECLARATIONS = "targetDecls"

  val defaultSchemaSource: TriggerModeType = SHAPEMAP
}
