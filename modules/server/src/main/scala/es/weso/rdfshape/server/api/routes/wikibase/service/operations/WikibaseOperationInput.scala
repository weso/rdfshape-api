package es.weso.rdfshape.server.api.routes.wikibase.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationDetails
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when asking the server
  * for most wikibase operations
  *
  * @param operationDetails Information required to perform the operation
  *                         on the Wikibase
  */
case class WikibaseOperationInput(operationDetails: WikibaseOperationDetails)

object WikibaseOperationInput
    extends ServiceRouteOperation[WikibaseOperationInput] {
  override implicit val decoder: Decoder[WikibaseOperationInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeOperationDetails <- cursor
          .as[Either[String, WikibaseOperationDetails]]

      } yield maybeOperationDetails.map(WikibaseOperationInput(_))

      mapEitherToDecodeResult(decodeResult)
    }
}
