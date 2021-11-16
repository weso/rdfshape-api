package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType.TriggerModeType
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{TargetDeclarations, ValidationTrigger}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Data class representing a validation trigger enabled by target declarations,
  * for SHACL validations.
  */
sealed case class TriggerTargetDeclarations private ()
    extends TriggerMode
    with LazyLogging {

  override val triggerModeType: TriggerModeType =
    TriggerModeType.TARGET_DECLARATIONS

  override def getValidationTrigger: Either[String, ValidationTrigger] =
    Right(TargetDeclarations)
}

private[api] object TriggerTargetDeclarations
    extends TriggerModeCompanion[TriggerTargetDeclarations]
    with LazyLogging {

  /** Given a request's parameters, try to extract a TriggerMode instance from them
    *
    * @param partsMap Request's parameters
    * @return Either the trigger mode or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap
  ): IO[Either[String, TriggerTargetDeclarations]] =
    IO.pure(Right(TriggerTargetDeclarations()))

  override implicit val encodeTriggerMode: Encoder[TriggerTargetDeclarations] =
    (tsm: TriggerTargetDeclarations) =>
      Json.obj(
        ("type", tsm.triggerModeType.asJson)
      )

  override implicit val decodeTriggerMode: Decoder[TriggerTargetDeclarations] =
    (_: HCursor) => Decoder.resultInstance.pure(TriggerTargetDeclarations())
}
