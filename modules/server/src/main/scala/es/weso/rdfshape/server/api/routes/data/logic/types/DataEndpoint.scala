package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  DataFormatParameter,
  EndpointParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.util.matching.Regex

/** RDF data obtained from a given endpoint
  *
  * @param endpoint   IRI with the RDF data
  * @param dataFormat Data format
  */
case class DataEndpoint(
    endpoint: IRI,
    dataFormat: DataFormat = DataFormat.defaultFormat
) extends Data
    with LazyLogging {

  override lazy val rawData: Either[String, String] = getUrlContents(
    endpoint.uri.toString
  )
  override val dataSource: DataSource     = DataSource.ENDPOINT
  override val format: Option[DataFormat] = Some(dataFormat)

  override def toRdf(
      relativeBase: Option[IRI]
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    RDFAsJenaModel.fromIRI(this.endpoint, dataFormat.name, relativeBase)
  }

  override def toString: String = {
    rawData.getOrElse("")
  }
}

private[api] object DataEndpoint extends DataCompanion[DataEndpoint] {
  override lazy val emptyData: DataEndpoint = DataEndpoint(defaultIri)
  private val defaultIri                    = IRI("http://www.example.org")

  /** Regular expressions used for identifying if a custom endpoint was given for this data sample
    */
  private val endpointRegex: Regex = "Endpoint: (.+)".r

  override implicit val encodeData: Encoder[DataEndpoint] =
    (data: DataEndpoint) =>
      Json.obj(
        ("endpoint", Json.fromString(data.endpoint.str)),
        ("source", Json.fromString(DataSource.ENDPOINT)),
        ("format", data.dataFormat.asJson)
      )

  override def mkData(partsMap: PartsMap): IO[Either[String, DataEndpoint]] =
    for {
      // Parse params
      paramEndpoint <- partsMap.optPartValue(EndpointParameter.name)
      paramFormat <- DataFormat.fromRequestParams(
        DataFormatParameter.name,
        partsMap
      )

      // Get final endpoint and format
      endpoint = getEndpoint(paramEndpoint)
      format   = paramFormat.getOrElse(ApiDefaults.defaultDataFormat)

      // Try to create data
      maybeData: Either[String, DataEndpoint] =
        if(endpoint.isDefined) {
          logger.debug(s"RDF Data received - Endpoint Data: ${endpoint.get}")
          IRI
            .fromString(endpoint.get)
            .fold(
              err => Left(s"Could not read endpoint data: $err"),
              iri => Right(DataEndpoint(iri, format))
            )

        } else Left("No endpoint provided")

    } yield maybeData.flatMap(_.rawData.flatMap(_ => maybeData))

  override implicit val decodeData: Decoder[DataEndpoint] = (cursor: HCursor) =>
    {
      for {
        endpoint <- cursor.downField("endpoint").as[String]

        dataFormat <- cursor
          .downField("format")
          .as[DataFormat]
          .orElse(Right(ApiDefaults.defaultDataFormat))

        base = DataEndpoint.emptyData.copy(
          endpoint = IRI.fromString(endpoint).getOrElse(defaultIri),
          dataFormat = dataFormat
        )

      } yield base
    }

  /** @param endpointStr  String containing the endpoint
    * @param endpointRegex Regex used to look for the endpoint in the string
    * @return Optionally, the endpoint contained in a given data string
    */
  private def getEndpoint(
      endpointStr: Option[String],
      endpointRegex: Regex = endpointRegex
  ): Option[String] = {
    endpointStr match {
      case None => None
      case Some(endpoint) =>
        endpoint match {
          case endpointRegex(endpoint) => Some(endpoint)
          case _                       => None
        }

    }
  }
}
