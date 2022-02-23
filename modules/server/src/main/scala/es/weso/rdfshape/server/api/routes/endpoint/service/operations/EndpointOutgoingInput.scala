package es.weso.rdfshape.server.api.routes.endpoint.service.operations

import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  EndpointParameter,
  QueryParameter
}
import es.weso.rdfshape.server.implicits.codecs.decodeUrl
import io.circe.{Decoder, HCursor}

import java.net.URL

case class EndpointOutgoingInput(endpoint: URL, query: SparqlQuery)

object EndpointOutgoingInput
    extends ServiceRouteOperation[EndpointOutgoingInput] {

  override implicit val decoder
      : Decoder[Either[String, EndpointOutgoingInput]] =
    (cursor: HCursor) => {
      for {
        maybeEndpoint <- cursor
          .downField(EndpointParameter.name)
          .as[Either[String, URL]]

        maybeQuery <- cursor
          .downField(QueryParameter.name)
          .as[Either[String, SparqlQuery]]

        decoded = for {
          endpoint <- maybeEndpoint
          query    <- maybeQuery
        } yield EndpointOutgoingInput(endpoint, query)

      } yield decoded
    }
}
