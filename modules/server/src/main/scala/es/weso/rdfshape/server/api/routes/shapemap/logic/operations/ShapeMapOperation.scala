package es.weso.rdfshape.server.api.routes.shapemap.logic.operations

import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap

/** General definition of operations that operate on [[ShapeMap]]s
  *
  * @param successMessage Message attached to the result of the operation
  * @param inputShapeMap  ShapeMap operated on
  */
private[operations] abstract class ShapeMapOperation(
    val successMessage: String = ShapeMapOperation.successMessage,
    val inputShapeMap: ShapeMap
)

/** Static utils for ShapeMap operations
  */
private[operations] object ShapeMapOperation {

  /** Dummy success message
    */
  private val successMessage = "Operation completed successfully"
}
