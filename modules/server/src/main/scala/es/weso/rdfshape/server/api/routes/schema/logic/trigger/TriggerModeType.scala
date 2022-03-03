package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import es.weso.rdfshape.server.utils.other.MyEnum

/** Enumeration of the different possible Validation Triggers sent by the client.
  * The trigger sent indicates the API how to proceed with validations
  */
private[api] object TriggerModeType extends MyEnum[String] {
  type TriggerModeType = String

  val SHAPEMAP            = "ShapeMap"
  val TARGET_DECLARATIONS = "TargetDecls"

  val values: Set[String]      = Set(SHAPEMAP, TARGET_DECLARATIONS)
  val default: TriggerModeType = SHAPEMAP
}
