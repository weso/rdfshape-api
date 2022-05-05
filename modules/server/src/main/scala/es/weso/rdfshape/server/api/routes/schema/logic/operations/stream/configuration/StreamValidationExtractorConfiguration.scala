package es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.configuration

import cats.implicits.toBifunctorOps
import es.weso.rdf.InferenceEngine
import es.weso.rdfshape.server.api.format.Format.FormatOps
import es.weso.rdfshape.server.api.format.dataFormats.{DataFormat, RdfFormat}
import es.weso.rdfshape.server.api.routes.data.logic.aux.InferenceCodecs.decodeInference
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ConcurrentItemsParameter,
  DataParameter,
  FormatParameter,
  InferenceParameter
}
import io.circe.{Decoder, HCursor}
import org.ragna.comet.data.{DataFormat => DataFormatComet}
import org.ragna.comet.stream.extractors.StreamExtractor

import scala.util.Try

/** Minor configuration class, containing the information required for a
  * Comet extractor to run
  *
  * Everything but the data format is optional or has a default value
  *
  * Extractor timeout is configured by the server to avoid abusing
  * the API
  *
  * @see [[StreamExtractor]]
  */
final case class StreamValidationExtractorConfiguration(
    dataFormat: DataFormat,
    dataInference: Option[InferenceEngine],
    concurrentItems: Option[Int]
) {
  // Pre-requisites:
  // 1. The data format supplied is available in the streaming library
  private lazy val maybeCometDataFormat = dataFormat.toStreamingDataFormat
  assume(
    maybeCometDataFormat.isRight,
    maybeCometDataFormat.left.getOrElse(
      StreamValidationConfiguration.unknownErrorMessage
    )
  )

  /** Make the final data format used by the streaming library publicly available,
    * now that its safe to access it
    */
  val cometDataFormat: DataFormatComet = maybeCometDataFormat.toOption.get
}

object StreamValidationExtractorConfiguration {

  /** Custom decoder fetching the correct parameter names in the incoming
    * JSON documents
    *
    * Interprets the incoming timeout value as a number of milliseconds
    */
  implicit val decoder
      : Decoder[Either[String, StreamValidationExtractorConfiguration]] =
    (cursor: HCursor) => {
      val configInfo = for {
        maybeDataFormat <- cursor
          .downField(DataParameter.name)
          .downField(FormatParameter.name)
          .as[Either[String, RdfFormat]]

        optInference <- cursor
          .downField(DataParameter.name)
          .downField(InferenceParameter.name)
          .as[Option[InferenceEngine]]

        optConcurrentItems <- cursor
          .downField(ConcurrentItemsParameter.name)
          .as[Option[Int]]
      } yield (
        maybeDataFormat,
        optInference,
        optConcurrentItems
      )

      configInfo.map {
        /* Destructure and try to build the object, catch the exception as error
         * message if needed */
        case (
              maybeDataFormat,
              optInference,
              optConcurrentItems
            ) =>
          for {
            dataFormat <- maybeDataFormat
            finalConfig <- Try {
              StreamValidationExtractorConfiguration(
                dataFormat,
                optInference,
                optConcurrentItems
              )
            }.toEither.leftMap(err =>
              s"Could not build the extractor configuration from user data:\n ${err.getMessage}"
            )
          } yield finalConfig
      }
    }
}
