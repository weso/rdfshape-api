package es.weso.rdfshape.server.api.routes.data.logic.data

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.data.DataSource.DataSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  DataFormatParameter,
  EndpointParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.util.matching.Regex

/** RDF data obtained from a given endpoint
  *
  * @param endpoint   IRI with the RDF data
  * @param dataFormat Data format
  */
case class EndpointData(
    endpoint: IRI,
    dataFormat: DataFormat = DataFormat.defaultFormat
) extends Data
    with LazyLogging {

  override val dataSource: DataSource = DataSource.ENDPOINT

  override def toRdf(
      relativeBase: Option[IRI]
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    RDFAsJenaModel.fromIRI(this.endpoint, dataFormat.name, relativeBase)
  }
}

private[api] object EndpointData extends DataCompanion[EndpointData] {
  override lazy val emptyData: EndpointData = EndpointData(IRI(defaultIri))
  private val defaultIri                    = "http://www.example.org"

  /** Regular expressions used for identifying if a custom endpoint was given for this data sample
    */
  private val endpointRegex: Regex = "Endpoint: (.+)".r

  override implicit val encodeData: Encoder[EndpointData] =
    (data: EndpointData) =>
      Json.obj(
        ("endpoint", Json.fromString(data.endpoint.str)),
        ("source", Json.fromString(DataSource.ENDPOINT)),
        ("format", data.dataFormat.asJson)
      )

  override implicit val decodeData: Decoder[EndpointData] = (cursor: HCursor) =>
    {
      for {
        endpoint <- cursor.downField("endpoint").as[String]

        dataFormat <- cursor
          .downField("format")
          .as[DataFormat]
          .orElse(Right(ApiDefaults.defaultDataFormat))

        base = EndpointData.emptyData.copy(
          endpoint = IRI.fromString(endpoint).getOrElse(defaultIri),
          dataFormat = dataFormat
        )

      } yield base
    }

  override def mkData(partsMap: PartsMap): IO[Either[String, EndpointData]] =
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
      maybeData: Either[String, EndpointData] = // 2. Endpoint data
        if(endpoint.isDefined) {
          logger.debug(s"RDF Data received - Endpoint Data: ${endpoint.get}")
          IRI
            .fromString(endpoint.get)
            .fold(
              err => Left(s"Could not read endpoint data: $err"),
              iri => Right(EndpointData(iri, format))
            )

        } else Left("No endpoint provided")
    } yield maybeData

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
