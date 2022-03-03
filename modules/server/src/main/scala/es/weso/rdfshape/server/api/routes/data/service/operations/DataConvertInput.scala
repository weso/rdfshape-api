package es.weso.rdfshape.server.api.routes.data.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  DataParameter,
  TargetFormatParameter
}
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for RDF data conversions
  * @param data RDF to be converted
  * @param targetFormat Conversion output format
  */
case class DataConvertInput(data: Data, targetFormat: DataFormat)

object DataConvertInput extends ServiceRouteOperation[DataConvertInput] {
  override implicit val decoder: Decoder[DataConvertInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeData <- cursor
          .downField(DataParameter.name)
          .as[Either[String, Data]]
        maybeTargetFormat <- cursor
          .downField(TargetFormatParameter.name)
          .as[Either[String, DataFormat]]

        maybeItems = for {
          data         <- maybeData
          targetFormat <- maybeTargetFormat
        } yield (data, targetFormat)

      } yield maybeItems.map { case (data, targetFormat) =>
        DataConvertInput(data, targetFormat)
      }

      mapEitherToDecodeResult(decodeResult)
    }
}
