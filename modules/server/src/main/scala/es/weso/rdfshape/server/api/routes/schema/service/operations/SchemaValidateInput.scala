package es.weso.rdfshape.server.api.routes.schema.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerMode
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for a schema validation of data
  *
  * @param data Data to be validated
  * @param schema Schema model for the validation
  * @param triggerMode Validation trigger, i.e.: how the validation was
  *                    started (shapeMap, target declarations...)
  */
case class SchemaValidateInput(
    data: Data,
    schema: Schema,
    triggerMode: TriggerMode
)

object SchemaValidateInput extends ServiceRouteOperation[SchemaValidateInput] {
  override implicit val decoder: Decoder[SchemaValidateInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeData <- cursor
          .downField(DataParameter.name)
          .as[Either[String, Data]]

        maybeSchema <- cursor
          .downField(SchemaParameter.name)
          .as[Either[String, Schema]]

        maybeTriggerMode <- cursor
          .downField(TriggerModeParameter.name)
          .as[Either[String, TriggerMode]](
            TriggerMode.decode(maybeData.toOption, maybeSchema.toOption)
          )

        maybeItems = for {
          data        <- maybeData
          schema      <- maybeSchema
          triggerMode <- maybeTriggerMode
        } yield (data, schema, triggerMode)

      } yield maybeItems.map { case (data, schema, triggerMode) =>
        SchemaValidateInput(data, schema, triggerMode)
      }
      mapEitherToDecodeResult(decodeResult)
    }
}
