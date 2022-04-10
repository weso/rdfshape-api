package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType._
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.TypeParameter
import es.weso.schema.ValidationTrigger
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

import scala.language.implicitConversions

/** Common trait to all validation triggers, whichever its nature
  */
trait TriggerMode {

  /** Corresponding type of this adapter inside [[ValidationTrigger]]
    */
  val `type`: TriggerModeType

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
}
