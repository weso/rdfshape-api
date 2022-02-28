package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType.TriggerModeType
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{TargetDeclarations, ValidationTrigger}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Data class representing a validation trigger enabled by target declarations,
  * for SHACL validations.
  */
sealed case class TriggerTargetDeclarations private (
    override val data: Option[Data] = None,
    override val schema: Option[Schema] = None
) extends TriggerMode
    with LazyLogging {

  override val _type: TriggerModeType =
    TriggerModeType.TARGET_DECLARATIONS

  override def getValidationTrigger: ValidationTrigger =
    TargetDeclarations
}

private[api] object TriggerTargetDeclarations
    extends TriggerModeCompanion[TriggerTargetDeclarations]
    with LazyLogging {

  override implicit def decode(
      data: Option[Data],
      schema: Option[Schema]
  ): Decoder[Either[String, TriggerTargetDeclarations]] =
    (_: HCursor) => TriggerTargetDeclarations(data, schema).asRight.asRight

  override implicit val encode: Encoder[TriggerTargetDeclarations] =
    (tsm: TriggerTargetDeclarations) =>
      Json.obj(
        ("type", tsm._type.asJson),
        ("data", tsm.data.asJson),
        ("schema", tsm.schema.asJson)
      )

  /** Given a request's parameters, try to extract a TriggerMode instance from them
    *
    * @param partsMap Request's parameters
    * @return Either the trigger mode or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap,
      data: Option[Data],
      schema: Option[Schema]
  ): IO[Either[String, TriggerTargetDeclarations]] =
    IO.pure(Right(TriggerTargetDeclarations(data, schema)))
}
