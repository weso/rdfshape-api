package es.weso.rdfshape.server.api.routes.schema.service.operations.stream.configuration

import cats.effect.unsafe.implicits.global
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerMode
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.schema.{Schema => SchemaW}
import io.circe.{Decoder, HCursor}
import org.ragna.comet.trigger.{ValidationTrigger => ValidationTriggerComet}
import org.ragna.comet.validation.Validator
import org.ragna.comet.validation.configuration.ValidatorConfiguration

/** Minor configuration class, containing the information required for a
  * Comet validator to run
  *
  * The conversion from RDFShape Schemas ([[Schema]]) to SHAclEX schemas
  * ([[SchemaW]]) is attempted whilst decoding the configuration instances
  * from client data, the same goes for ValidationTriggers
  *
  * @see [[Validator]]
  * @see [[ValidatorConfiguration]]
  */
final case class StreamValidationValidatorConfiguration(
    schema: SchemaW,
    trigger: ValidationTriggerComet,
    haltOnInvalid: Option[Boolean],
    haltOnErrored: Option[Boolean],
    concurrentItems: Option[Int]
)

object StreamValidationValidatorConfiguration {

  /** Custom decoder fetching the correct parameter names in the incoming
    * JSON documents
    *
    * Interprets the incoming timeout value as a number of milliseconds
    */
  implicit val decoder
      : Decoder[Either[String, StreamValidationValidatorConfiguration]] =
    (cursor: HCursor) => {
      val configInfo = for {
        maybeSchema <- cursor
          .downField(SchemaParameter.name)
          .as[Either[String, Schema]]

        maybeTriggerMode <- cursor
          .downField(TriggerModeParameter.name)
          .as[Either[String, TriggerMode]](
            TriggerMode.decode(None, maybeSchema.toOption)
          )

        optHaltInvalid <- cursor
          .downField(HaltOnInvalidParameter.name)
          .as[Option[Boolean]]

        optHaltErrored <- cursor
          .downField(HaltOnErroredParameter.name)
          .as[Option[Boolean]]

        optConcurrentItems <- cursor
          .downField(ConcurrentItemsParameter.name)
          .as[Option[Int]]

      } yield (
        maybeSchema,
        maybeTriggerMode,
        optHaltInvalid,
        optHaltErrored,
        optConcurrentItems
      )

      configInfo.map {
        case (
              maybeSchema,
              maybeTriggerMode,
              optHaltInvalid,
              optHaltErrored,
              optConcurrentItems
            ) =>
          for {
            rdfShapeSchema  <- maybeSchema
            rdfShapeTrigger <- maybeTriggerMode
            // Form final schema trigger used in the stream validation
            // Unsafe run due to SHAclEX API limitations
            cometSchema <- rdfShapeSchema.getSchema.unsafeRunSync()
            // Form final validation trigger used in the stream validation
            cometTrigger = rdfShapeTrigger.toStreamingTriggerMode
          } yield StreamValidationValidatorConfiguration(
            cometSchema,
            cometTrigger,
            optHaltInvalid,
            optHaltErrored,
            optConcurrentItems
          )
      }
    }
}
