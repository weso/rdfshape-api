package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType._
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.TriggerModeParameter
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.ValidationTrigger
import io.circe.{Decoder, Encoder, HCursor}

/** Common trait to all schemas, whichever its nature
  */
trait TriggerMode {

  /** Corresponding type of this adapter inside [[ValidationTrigger]]
    */
  val triggerModeType: TriggerModeType

  /** Optionally, the [[Data]] being validated in the validation using this trigger
    */
  val data: Option[Data]

  /** Optionally, the [[Schema]] being used in the validation using this trigger
    */
  val schema: Option[Schema]

  /** Get the inner [[ValidationTrigger]], which is used internally for schema validations
    *
    * @return Either the inner [[ValidationTrigger]] logical model as used by WESO libraries,
    *         or an error extracting the model
    */
  def getValidationTrigger: Either[String, ValidationTrigger]
}

object TriggerMode extends TriggerModeCompanion[TriggerMode] {

  /** Dummy implementation meant to be overridden.
    * If called on a general [[TriggerMode]] instance, pattern match among the available types to
    * use the correct implementation
    */
  override implicit val encoder: Encoder[TriggerMode] = {
    case tsm: TriggerShapeMap => TriggerShapeMap.encoder(tsm)
    case ttd: TriggerTargetDeclarations =>
      TriggerTargetDeclarations.encoder(ttd)
  }

  /** Dummy implementation meant to be overridden
    * If called on a general [[TriggerMode]] instance, pattern match among the available types to
    * use the correct implementation
    */
  override implicit val decoder: Decoder[TriggerMode] =
    (cursor: HCursor) => {
      this.getClass match {
        case tsm if tsm == classOf[TriggerShapeMap] =>
          TriggerShapeMap.decoder(cursor)
        case ttd if ttd == classOf[TriggerTargetDeclarations] =>
          TriggerTargetDeclarations.decoder(cursor)
      }
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

  /** Encoder used to transform [[TriggerMode]] instances to JSON values
    */
  implicit val encoder: Encoder[T]

  /** Decoder used to extract [[TriggerMode]] instances from JSON values
    */
  implicit val decoder: Decoder[T]

  /** Given a request's parameters, try to extract an instance of [[TriggerMode]] (type [[T]]) from them
    *
    * @param partsMap Request's parameters
    * @param data Optionally, the [[Data]] being validated in the validation using this trigger
    * @param schema Optionally, the [[Schema]] being used in the validation using this trigger
    * @return Either the [[TriggerMode]] instance or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap,
      data: Option[Data],
      schema: Option[Schema]
  ): IO[Either[String, T]]
}
