package es.weso.rdfshape.server.api.routes.schema.logic.operations

import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema

/** General definition of operations that operate on [[Schema]]s
  *
  * @param successMessage Message attached to the result of the operation
  * @param inputSchema    Schema operated on
  */
private[operations] abstract class SchemaOperation(
    val successMessage: String = SchemaOperation.successMessage,
    val inputSchema: Schema
)

private[operations] object SchemaOperation {

  /** Dummy success message
    */
  private val successMessage = "Operation completed successfully"
}
