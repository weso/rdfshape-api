package es.weso.rdfshape.server.api.routes.schema.service.operations

import cats.implicits.catsSyntaxEitherId
import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.schema.logic.aux.SchemaAdapter.decodeEngine
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  SchemaParameter,
  TargetEngineParameter,
  TargetFormatParameter
}
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import es.weso.schema.{Schema => SchemaW}
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for a schema conversion
  *
  * @param schema Schema to be inspected
  * @param targetFormat Desired output format
  * @param targetEngine Desired output engine
  */
case class SchemaConvertInput(
    schema: Schema,
    targetFormat: DataFormat,
    targetEngine: SchemaW
)

object SchemaConvertInput extends ServiceRouteOperation[SchemaConvertInput] {
  override implicit val decoder: Decoder[SchemaConvertInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeSchema <- cursor
          .downField(SchemaParameter.name)
          .as[Either[String, Schema]]

        // Use data format because target formats range
        // from SchemaFormats to GraphicFormats, etc.
        maybeTargetFormat <- cursor
          .downField(TargetFormatParameter.name)
          .as[Either[String, DataFormat]]

        maybeTargetEngine <- cursor
          .downField(TargetEngineParameter.name)
          .as[Either[String, SchemaW]] match {
          // Decoding error: the param was not sent. Use input schema engine
          case Left(_) => maybeSchema.map(_.engine).asRight
          // Keep the result extracted from user input
          case other => other
        }

        maybeItems = for {
          schema       <- maybeSchema
          targetFormat <- maybeTargetFormat
          targetEngine <- maybeTargetEngine
        } yield (schema, targetFormat, targetEngine)

      } yield maybeItems.map { case (schema, targetFormat, targetEngine) =>
        SchemaConvertInput(schema, targetFormat, targetEngine)
      }

      mapEitherToDecodeResult(decodeResult)
    }
}
