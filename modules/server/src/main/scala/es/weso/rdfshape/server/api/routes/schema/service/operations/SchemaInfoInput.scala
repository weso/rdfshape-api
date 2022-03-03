package es.weso.rdfshape.server.api.routes.schema.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.SchemaParameter
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for a schema information
  *
  * @param schema Schema to be inspected
  */
case class SchemaInfoInput(schema: Schema)

object SchemaInfoInput extends ServiceRouteOperation[SchemaInfoInput] {
  override implicit val decoder: Decoder[SchemaInfoInput] = (cursor: HCursor) =>
    {
      val decodeResult = for {
        maybeSchema <- cursor
          .downField(SchemaParameter.name)
          .as[Either[String, Schema]]
      } yield maybeSchema.map(SchemaInfoInput(_))

      mapEitherToDecodeResult(decodeResult)
    }
}
