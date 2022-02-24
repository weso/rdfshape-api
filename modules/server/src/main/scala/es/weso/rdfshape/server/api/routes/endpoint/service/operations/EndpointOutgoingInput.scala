package es.weso.rdfshape.server.api.routes.endpoint.service.operations

import cats.implicits.catsSyntaxEitherId
import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  EndpointParameter,
  QueryParameter
}
import es.weso.rdfshape.server.implicits.codecs.decodeUrl
import io.circe.{Decoder, DecodingFailure, HCursor}

import java.net.URL

case class EndpointOutgoingInput(endpoint: URL, query: SparqlQuery)

object EndpointOutgoingInput
    extends ServiceRouteOperation[EndpointOutgoingInput] {

  override implicit val decoder: Decoder[EndpointOutgoingInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeEndpoint <- cursor
          .downField(EndpointParameter.name)
          .as[Either[String, URL]]

        maybeQuery <- cursor
          .downField(QueryParameter.name)
          .as[Either[String, SparqlQuery]]

        decoded: Either[String, EndpointOutgoingInput] = for {
          endpoint <- maybeEndpoint
          query    <- maybeQuery
        } yield EndpointOutgoingInput(endpoint, query)

      } yield decoded

      /* If a native decoding failure occurred, leave it as is, else if an error
       * occurred return it as a decoding failure , else return the value */
      decodeResult.fold(
        _.asLeft,
        {
          case Left(errStr) => DecodingFailure(errStr, Nil).asLeft
          case Right(value) => value.asRight
        }
      )
    }
}
