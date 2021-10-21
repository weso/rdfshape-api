package es.weso.rdfshape.server.api.routes.data.logic.operations

import es.weso.rdfshape.server.api.routes.data.logic.operations.DataOperation.successMessage
import es.weso.rdfshape.server.api.routes.data.logic.types.Data

/** General definition of operations that operate on Data
  *
  * @param successMessage Message attached to the result of the operation
  * @param inputData    Data operated on
  */
abstract class DataOperation(
    val successMessage: String = successMessage,
    val inputData: Data
)

object DataOperation {

  /** Dummy success message
    */
  private val successMessage = "Operation completed successfully"
}
