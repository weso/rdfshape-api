package es.weso.rdfshape.server.api.routes.data.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.DataParameter
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * for RDF data information
  * @param data RDF to be inspected
  */
case class DataInfoInput(data: Data)

object DataInfoInput extends ServiceRouteOperation[DataInfoInput] {
  override implicit val decoder: Decoder[DataInfoInput] = (cursor: HCursor) => {
    val decodeResult = for {
      maybeData <- cursor
        .downField(DataParameter.name)
        .as[Either[String, Data]]
    } yield maybeData.map(DataInfoInput(_))

    mapEitherToDecodeResult(decodeResult)
  }
}
