package es.weso.rdfshape.server.api.results

import es.weso.rdfshape.server.utils.json.JsonUtilsServer.maybeField
import es.weso.shapemaps.ShapeMap
import io.circe.Json
import io.circe.syntax._

/** Data class representing the output of an conversion operation (input schema -> output schema)
  *
  * @param msg                Output informational message after processing. Used in case of error.
  * @param schema             Input schema
  * @param schemaFormat       Input schema format
  * @param schemaEngine       Input schema engine
  * @param targetSchemaFormat Target schema format
  * @param targetSchemaEngine Target schema engine
  * @param result             Output schema
  * @param resultShapeMap     Output shapemap
  */
case class SchemaConversionResult(
    msg: String,
    schema: Option[String],
    schemaFormat: Option[String],
    schemaEngine: Option[String],
    targetSchemaFormat: Option[String],
    targetSchemaEngine: Option[String],
    result: Option[String],
    resultShapeMap: Option[ShapeMap]
) {

  /** Convert a conversion result to its JSON representation
    *
    * @return JSON information of the conversion result
    */
  def toJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg))
    ) ++
      maybeField(schema, "schema", Json.fromString) ++
      maybeField(schemaFormat, "schemaFormat", Json.fromString) ++
      maybeField(schemaEngine, "schemaEngine", Json.fromString) ++
      maybeField(targetSchemaFormat, "targetSchemaFormat", Json.fromString) ++
      maybeField(targetSchemaEngine, "targetSchemaEngine", Json.fromString) ++
      maybeField(result, "result", Json.fromString) ++
      maybeField(
        resultShapeMap,
        "shapeMap",
        (sm: ShapeMap) => sm.toString.asJson
      )
  )
}

object SchemaConversionResult {

  /** @param msg Error message contained in the result
    * @return A SchemaConversionResult consisting of a single error message and no data
    */
  def fromMsg(msg: String): SchemaConversionResult =
    SchemaConversionResult(msg, None, None, None, None, None, None, None)

  /** @return A SchemaConversionResult, given all the parameters needed to build it (schemas, formats, results, etc.)
    */
  def fromConversion(
      source: String,
      schemaFormat: String,
      schemaEngine: String,
      optTargetSchemaFormat: Option[String],
      optTargetSchemaEngine: Option[String],
      result: String,
      resultShapeMap: ShapeMap
  ): SchemaConversionResult =
    SchemaConversionResult(
      s"Conversion ($schemaFormat/$schemaEngine) => (${optTargetSchemaFormat
        .getOrElse("")}/${optTargetSchemaEngine.getOrElse("")}) successful",
      Some(source),
      Some(schemaFormat),
      Some(schemaEngine),
      optTargetSchemaFormat,
      optTargetSchemaEngine,
      Some(result),
      Some(resultShapeMap)
    )
}
