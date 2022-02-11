package es.weso.rdfshape.server.api.routes.schema.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.routes.schema.logic.aux.SchemaAdapter._
import es.weso.rdfshape.server.api.routes.schema.logic.operations.SchemaInfo.SchemaInfoResult
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.utils.json.JsonUtils.prefixMap2JsonArray
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a schema-information operation
  *
  * @param inputSchema Schema used as input of the operation
  * @param result      [[SchemaInfoResult]] containing the resulting schema information
  */
final case class SchemaInfo private (
    override val inputSchema: Schema,
    result: SchemaInfoResult
) extends SchemaOperation(SchemaInfo.successMessage, inputSchema)

private[api] object SchemaInfo extends LazyLogging {

  private val successMessage = "Well formed Schema"

  /** Given an input schema, get information about it
    *
    * @param schema Input schema instance of any type
    * @return A [[SchemaInfo]] instance with the information of the input schema
    */

  def schemaInfo(schema: Schema): IO[SchemaInfo] = for {
    model <- schema.getSchema
    modelInfo = model.map(m => (m.shapes, m.pm))

    results <- modelInfo match {
      case Right((shapes, prefixMap)) =>
        IO {
          SchemaInfo(
            inputSchema = schema,
            result = SchemaInfoResult(
              schema = schema,
              shapes = shapes,
              prefixMap = prefixMap
            )
          )
        }
      case Left(err) =>
        IO.raiseError(new RuntimeException(err))
    }
  } yield results

  /** Case class representing the results to be returned when performing a schema-info operation
    *
    * @param schema    Schema operated on
    * @param shapes    Shapes in the schema
    * @param prefixMap Prefix map in the schema
    */
  final case class SchemaInfoResult private (
      schema: Schema,
      shapes: List[String],
      prefixMap: PrefixMap
  )

  /** JSON encoder for [[SchemaInfoResult]]
    */
  private implicit val encodeSchemaInfoResult: Encoder[SchemaInfoResult] =
    (schemaInfoResult: SchemaInfoResult) =>
      Json.fromFields(
        List(
          ("format", schemaInfoResult.schema.format.asJson),
          ("engine", schemaInfoResult.schema.engine.asJson),
          (
            "shapes",
            Json.fromValues(schemaInfoResult.shapes.map(Json.fromString))
          ),
          ("prefixMap", prefixMap2JsonArray(schemaInfoResult.prefixMap))
        )
      )

  /** JSON encoder for [[SchemaInfo]]
    */
  implicit val encodeSchemaInfoOperation: Encoder[SchemaInfo] =
    (schemaInfo: SchemaInfo) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(schemaInfo.successMessage)),
          ("schema", schemaInfo.inputSchema.asJson),
          ("result", schemaInfo.result.asJson)
        )
      )
}
