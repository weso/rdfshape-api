package es.weso.rdfshape.server.api.routes.data.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  DataParameter,
  QueryParameter
}
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Data class representing the inputs required when querying the server
  * to perform a SPARQL query on RDF data
  *
  * @param data RDF to be queried
  * @param query SPARQL query to be performed
  */
case class DataQueryInput(data: Data, query: SparqlQuery)

object DataQueryInput extends ServiceRouteOperation[DataQueryInput] {
  override implicit val decoder: Decoder[DataQueryInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeData <- cursor
          .downField(DataParameter.name)
          .as[Either[String, Data]]
        maybeQuery <- cursor
          .downField(QueryParameter.name)
          .as[Either[String, SparqlQuery]]

        maybeItems = for {
          data  <- maybeData
          query <- maybeQuery
        } yield (data, query)

      } yield maybeItems.map { case (data, query) =>
        DataQueryInput(data, query)
      }

      mapEitherToDecodeResult(decodeResult)
    }
}
