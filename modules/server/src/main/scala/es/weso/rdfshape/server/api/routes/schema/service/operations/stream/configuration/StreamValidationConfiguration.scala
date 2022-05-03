package es.weso.rdfshape.server.api.routes.schema.service.operations.stream.configuration

import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ExtractorParameter,
  StreamParameter,
  ValidatorParameter
}
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, HCursor}

/** Broad configuration class containing all the child configurations required to
  * perform a Stream validation with Comet
  *
  * @param validatorConfiguration Configuration used by the eventual validator
  * @param extractorConfiguration Configuration used by the eventual extractor
  * @param streamConfiguration    Configuration used to fetch the input data stream
  */
sealed case class StreamValidationConfiguration(
    validatorConfiguration: StreamValidationValidatorConfiguration,
    extractorConfiguration: StreamValidationExtractorConfiguration,
    streamConfiguration: StreamValidationStreamConfiguration
)

object StreamValidationConfiguration {

  /** Custom decoder fetching the correct parameter names in the incoming
    * JSON documents
    */
  implicit val decoder: Decoder[StreamValidationConfiguration] =
    (cursor: HCursor) => {
      val decodeResult = for {
        validatorConfig <- cursor
          .downField(ValidatorParameter.name)
          .as[Either[String, StreamValidationValidatorConfiguration]]

        extractorConfig <- cursor
          .downField(ExtractorParameter.name)
          .as[Either[String, StreamValidationExtractorConfiguration]]

        streamConfig <- cursor
          .downField(StreamParameter.name)
          .as[Either[String, StreamValidationStreamConfiguration]]

        // Cumulative eithers
        configItems = for {
          vc <- validatorConfig
          ec <- extractorConfig
          sc <- streamConfig
        } yield (vc, ec, sc)

      } yield configItems.map {
        case (validatorConfig, extractorConfig, streamConfig) =>
          StreamValidationConfiguration(
            validatorConfig,
            extractorConfig,
            streamConfig
          )
      }

      // Map any error to a DecodingFailure in the final step of decoding
      mapEitherToDecodeResult(decodeResult)
    }

  /** Generic error message used as placeholder when no error cause is available
    */
  private[configuration] val unknownErrorMessage: String =
    "Unknown error creating the configuration for stream validation"
}
