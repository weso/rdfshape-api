package es.weso.rdfshape.server.api.routes.schema.logic.operations

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.operations.SchemaInfo.SchemaInfoResult
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.{
  TriggerMode,
  TriggerModeType,
  TriggerShapeMap,
  TriggerTargetDeclarations
}
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.schema.{Result => ValidationResult}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a schema-data-validation operation
  *
  * @param inputSchema       Schema used as input of the operation
  * @param validationTrigger [[TriggerShapeMap]] triggering the operation
  * @param result            [[SchemaInfoResult]] containing the resulting schema information
  */
final case class SchemaValidate private (
    inputData: Data,
    override val inputSchema: Schema,
    validationTrigger: TriggerMode,
    result: ValidationResult
) extends SchemaOperation(SchemaValidate.successMessage, inputSchema)

private[api] object SchemaValidate extends LazyLogging {

  /** Convert a [[ValidationResult]] to its JSON representation
    *
    * @note Exceptionally uses unsafeRun
    */
  implicit val encodeValidationResult: Encoder[ValidationResult] =
    (validationResult: ValidationResult) =>
      RDFAsJenaModel.empty
        .flatMap(_.use(validationResult.toJson(_)))
        .unsafeRunSync()

  /** Convert a [[SchemaValidate]] to its JSON representation
    *
    * @return JSON representation of the validation operation,
    *         used for API responses
    * @note
    */
  implicit val encodeSchemaValidateOperation: Encoder[SchemaValidate] =
    (schemaValidate: SchemaValidate) => {

      // Convert ValidationResult to JSON
      val validationResultJson: IO[Json] = for {
        emptyResource <- RDFAsJenaModel.empty
        json          <- emptyResource.use(schemaValidate.result.toJson(_))
      } yield json

      Json.fromFields(
        List(
          ("message", Json.fromString(schemaValidate.successMessage)),
          ("data", schemaValidate.inputData.asJson),
          ("schema", schemaValidate.inputSchema.asJson),
          ("trigger", schemaValidate.validationTrigger.asJson),
          // UnsafeRun exceptionally.
          ("result", validationResultJson.unsafeRunSync())
        )
      )
    }
  private val successMessage = "Validation successful"

  /** For a given RDF [[Data]] plus a given [[Schema]], attempt to validate
    * the data according to the schema using WESO libraries.
    *
    * @param data    [[Data]] to be validated
    * @param schema  [[Schema]] used for validation
    * @param trigger [[TriggerMode]] indicating the origin and the type of
    *                validation being performed (see [[TriggerModeType]]
    * @return A [[SchemaValidate]] instance holding the validation inputs and results
    */
  def schemaValidate(
      data: Data,
      schema: Schema,
      trigger: TriggerMode
  ): IO[SchemaValidate] = for {
    // Get a builder and the data reasoner
    builderResource <- RDFAsJenaModel.empty
    rdfResource     <- data.toRdf()
    result <- (builderResource, rdfResource).tupled.use { case (builder, rdf) =>
      trigger match {
        // ShEx validation with Shapemap |
        // SHACL validation with target declarations
        case _: TriggerShapeMap | _: TriggerTargetDeclarations =>
          for {
            innerSchema <- schema.getSchema
            result = innerSchema.map(
              _.validate(rdf, trigger.getValidationTrigger, builder)
            )
            validation <- result match {
              // Check for Lefts, errors while getting the schema, triggers...
              case Left(err) => IO.raiseError(new RuntimeException(err))
              /* If no errors are found here, we can return the IO performing
               * the validation */
              case Right(validationOp) => validationOp
            }
          } yield validation

        // Invalid trigger type, exit
        case other =>
          IO.raiseError(
            new RuntimeException(
              s"Unexpected validation trigger (${other._type})"
            )
          )
      }
    }
  } yield SchemaValidate(
    inputData = data,
    inputSchema = schema,
    validationTrigger = trigger,
    result = result
  )
}
