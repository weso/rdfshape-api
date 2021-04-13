package es.weso.server.results

import es.weso.shapemaps.{ResultShapeMap, ShapeMap}
import es.weso.utils.json.JsonUtilsServer.{maybeField, _}
import io.circe.Json
import io.circe.syntax._

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
  def fromMsg(msg: String): SchemaConversionResult =
    SchemaConversionResult(msg, None, None, None, None, None, None, None)
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
