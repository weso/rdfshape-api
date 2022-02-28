package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType._
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  TriggerModeParameter,
  TypeParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.ValidationTrigger
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

import scala.language.implicitConversions

/** Common trait to all schemas, whichever its nature
  */
trait TriggerMode {

  /** Corresponding type of this adapter inside [[ValidationTrigger]]
    */
  val _type: TriggerModeType

  /** Optionally, the [[Data]] being validated in the validation using this trigger
    */
  val data: Option[Data]

  /** Optionally, the [[Schema]] being used in the validation using this trigger
    */
  val schema: Option[Schema]

  /** Get the inner [[ValidationTrigger]], which is used internally for schema validations
    *
    * @return The inner [[ValidationTrigger]] logical model as used by WESO libraries
    */
  def getValidationTrigger: ValidationTrigger
}

object TriggerMode extends TriggerModeCompanion[TriggerMode] {

  /** Dummy implementation meant to be overridden
    * If called on a general [[TriggerMode]] instance, look for the type to
    * redirecting the decoding to the correct implementation
    */
  override implicit def decode(
      data: Option[Data] = None,
      schema: Option[Schema] = None
  ): Decoder[Either[String, TriggerMode]] =
    (cursor: HCursor) => {
      for {
        triggerType <- cursor
          .downField(TypeParameter.name)
          .as[TriggerModeType]

        decoded <- triggerType match {
          case SHAPEMAP => TriggerShapeMap.decode(data, schema)(cursor)
          case TARGET_DECLARATIONS =>
            TriggerTargetDeclarations.decode(data, schema)(cursor)
          case _ =>
            DecodingFailure(
              s"Invalid trigger mode type '$triggerType'",
              Nil
            ).asLeft
        }
      } yield decoded
    }

  /** Dummy implementation meant to be overridden.
    * If called on a general [[TriggerMode]] instance, pattern match among the available types to
    * use the correct implementation
    */
  override implicit val encode: Encoder[TriggerMode] = {
    case tsm: TriggerShapeMap => TriggerShapeMap.encode(tsm)
    case ttd: TriggerTargetDeclarations =>
      TriggerTargetDeclarations.encode(ttd)
  }

  /** General implementation delegating on subclasses
    */
  override def mkTriggerMode(
      partsMap: PartsMap,
      data: Option[Data] = None,
      schema: Option[Schema] = None
  ): IO[Either[String, TriggerMode]] =
    for {
      /* 1. Make some checks on the parameters to distinguish between
       * TriggerMode types */
      triggerModeType <- partsMap.optPartValue(TriggerModeParameter.name)
      // 2. Delegate on the correct sub-class for creating the Schema
      maybeTriggerMode <- triggerModeType match {
        // A triggerMode was sent, pattern match to all possibilities
        case Some(triggerModeStr) =>
          triggerModeStr match {
            // ShapeMap: ShEx validation
            case SHAPEMAP =>
              TriggerShapeMap.mkTriggerMode(partsMap, data, schema)
            // TargetDecls: SHACL validation
            case TARGET_DECLARATIONS =>
              TriggerTargetDeclarations.mkTriggerMode(partsMap, data, schema)
            // Invalid value received for trigger mode
            case _ => IO.pure(Left("Invalid value received for trigger mode"))
          }
        // No trigger mode was sent, error
        case None => IO.pure(Left("Could not find a trigger mode"))
      }
    } yield maybeTriggerMode
}

/** Static utilities to be used with [[TriggerMode]] representations
  *
  * @tparam T Specific [[TriggerMode]] representation to be handled
  */
private[schema] trait TriggerModeCompanion[T <: TriggerMode]
    extends LazyLogging {

  /** @param data Optional data accompanying the ShapeMap
    * @param schema Optional schema accompanying the ShapeMap
    * @return Decoding function used to extract [[TriggerMode]] instances from JSON values
    */
  implicit def decode(
      data: Option[Data],
      schema: Option[Schema]
  ): Decoder[Either[String, T]]

  /** Encoder used to transform [[TriggerMode]] instances to JSON values
    */
  implicit val encode: Encoder[T]

  /** Given a request's parameters, try to extract an instance of [[TriggerMode]] (type [[T]]) from them
    *
    * @param partsMap Request's parameters
    * @param data     Optionally, the [[Data]] being validated in the validation using this trigger
    * @param schema   Optionally, the [[Schema]] being used in the validation using this trigger
    * @return Either the [[TriggerMode]] instance or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap,
      data: Option[Data],
      schema: Option[Schema]
  ): IO[Either[String, T]]
}
