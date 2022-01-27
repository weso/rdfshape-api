package es.weso.rdfshape.server.api.routes.data.logic.operations

import es.weso.rdfshape.server.api.routes.data.logic.types.Data

/** General definition of operations that operate on Data
  *
  * @param successMessage Message attached to the result of the operation
  * @param inputData      Data operated on
  */
private[operations] abstract class DataOperation(
    val successMessage: String = DataOperation.successMessage,
    val inputData: Data
)

/** Static utils for Data operations
  */
private[operations] object DataOperation {

  /** Dummy success message
    */
  private val successMessage = "Operation completed successfully"
}
