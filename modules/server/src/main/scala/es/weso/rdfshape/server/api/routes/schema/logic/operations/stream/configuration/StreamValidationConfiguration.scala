package es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.configuration

import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ExtractorParameter,
  StreamParameter,
  ValidatorParameter
}
import es.weso.rdfshape.server.utils.other.mapEitherToDecodeResult
import io.circe.{Decoder, Error, HCursor}

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

  /** Error message used when a valid configuration can not be extracted from
    * the provided JSON data
    *
    * @param reason Error why the JSON decoding of the configuration failed
    * @return An informational text message containing some context and the
    *         provided error
    */
  private[schema] def invalidStreamValidationConfigurationReceived(
      reason: Option[Error]
  ) = {
    val formattedReasonText =
      reason.map(r => s": ${r.getMessage}").getOrElse("")
    s"Invalid configuration received for the stream validation$formattedReasonText"
  }
}
