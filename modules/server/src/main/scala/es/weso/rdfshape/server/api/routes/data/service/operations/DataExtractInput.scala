package es.weso.rdfshape.server.api.routes.data.service.operations

import cats.implicits.catsSyntaxEitherId
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.ServiceRouteOperation
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.data.service.DataServiceError
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  DataParameter,
  LabelParameter,
  NodeSelectorParameter
}
import es.weso.rdfshape.server.implicits.codecs.decodeIri
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, DecodingFailure, HCursor}

/** Data class representing the inputs required when querying the server
  * to extract a schema from RDF data
  *
  * @param data         RDF input
  * @param nodeSelector Node targeted in the extraction process
  * @param label        Base label for the extracted shapes
  */
case class DataExtractInput(
    data: Data,
    nodeSelector: String,
    label: Option[IRI]
)

object DataExtractInput extends ServiceRouteOperation[DataExtractInput] {
  override implicit val decoder: Decoder[DataExtractInput] =
    (cursor: HCursor) => {
      val decodeResult = for {
        maybeData <- cursor
          .downField(DataParameter.name)
          .as[Either[String, Data]]

        // Compulsory param, manually trigger decoding failure with message
        maybeNodeSelector <- cursor
          .downField(NodeSelectorParameter.name)
          .as[Option[String]]
          .map {
            case None => DataServiceError.noNodeSelector.asLeft
            case Some(value) if value.isBlank =>
              DataServiceError.emptyNodeSelector.asLeft
            case Some(value) => value.asRight
          }

        // Optional param
        maybeLabel <- cursor
          .downField(LabelParameter.name)
          .as[Either[String, IRI]] match {
          // Failed to decode at cursor, no label was provided
          case Left(_) => None.asRight
          // Could decode
          case Right(labelDecodeResult) =>
            labelDecodeResult match {
              // Decoded but an error message was returned, promote it
              case Left(err) => DecodingFailure(err, Nil).asLeft
              // Decoded and everything went OK
              case Right(label) => Some(label).asRight
            }
        }

        maybeItems = for {
          data         <- maybeData
          nodeSelector <- maybeNodeSelector
        } yield (data, nodeSelector, maybeLabel)

      } yield maybeItems.map { case (data, nodeSelector, label) =>
        DataExtractInput(data, nodeSelector, label)
      }

      mapEitherToDecodeResult(decodeResult)
    }
}
