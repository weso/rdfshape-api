package es.weso.rdfshape.server.api.routes.wikibase.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationDetails
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.SchemaParameter
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when asking the server
  * to validate Wikibase data
  *
  * @param operationDetails Information required to perform the operation
  *                         on the Wikibase
  * @param schema Schema used for validation
  */
case class WikibaseValidateInput(
    operationDetails: WikibaseOperationDetails,
    schema: Schema
)

object WikibaseValidateInput
    extends ServiceRouteOperation[WikibaseValidateInput] {

  override implicit val decoder: Decoder[WikibaseValidateInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeOperationDetails <- cursor
          .as[Either[String, WikibaseOperationDetails]]

        maybeSchema <- cursor
          .downField(SchemaParameter.name)
          .as[Either[String, Schema]]

        maybeItems = for {
          details <- maybeOperationDetails
          schema  <- maybeSchema
        } yield (details, schema)

      } yield maybeItems.map { case (opDetails, schema) =>
        WikibaseValidateInput(opDetails, schema)
      }

      mapEitherToDecodeResult(decodeResult)
    }
}
