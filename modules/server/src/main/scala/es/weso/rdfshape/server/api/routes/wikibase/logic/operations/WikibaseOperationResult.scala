package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikibase
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Case class representing the results to be returned when performing a search
  * operation in a wikibase instance
  *
  * @param operationData Input data operated on
  * @param wikibase      Target wikibase operated on
  * @param result        Results returned by the operation, ready for API responses
  */
final case class WikibaseOperationResult private (
    operationData: WikibaseOperationDetails,
    wikibase: Wikibase,
    result: Json
)

/** Static codec utilities for the results
  */
private[api] object WikibaseOperationResult {

  /** JSON encoder for [[WikibaseOperationResult]]s
    */
  implicit val encode: Encoder[WikibaseOperationResult] =
    (opResult: WikibaseOperationResult) =>
      Json.fromFields(
        List(
          ("operationData", opResult.operationData.asJson),
          ("wikibase", opResult.wikibase.asJson),
          ("result", opResult.result)
        )
      )
}
