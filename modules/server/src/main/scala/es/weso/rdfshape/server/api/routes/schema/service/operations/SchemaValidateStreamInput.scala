package es.weso.rdfshape.server.api.routes.schema.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.configuration.StreamValidationConfiguration
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for a schema validation of a stream of data using Comet
  *
  * @param configuration Configuration object with all settings needed by Comet
  *                      to fetch and validate a stream of data
  */
case class SchemaValidateStreamInput(
    configuration: StreamValidationConfiguration
)

object SchemaValidateStreamInput
    extends ServiceRouteOperation[SchemaValidateStreamInput] {

  override implicit val decoder: Decoder[SchemaValidateStreamInput] =
    (cursor: HCursor) => {
      for {
        // Decode all of the validation settings to a domain object,
        // relying on the decoders of each underlying configuration class
        validationConfig <- cursor
          .downField(ConfigurationParameter.name)
          .as[StreamValidationConfiguration]
      } yield SchemaValidateStreamInput(validationConfig)
    }
}
