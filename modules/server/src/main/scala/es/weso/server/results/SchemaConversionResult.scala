package es.weso.server.results

import es.weso.shapeMaps.{ResultShapeMap, ShapeMap}
import es.weso.utils.json.JsonUtilsServer.{maybeField, _}
import io.circe.Json

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
     ("msg", Json.fromString(msg)),
    ) ++
    maybeField(schema,"schema", Json.fromString(_)) ++
    maybeField(schemaFormat,"schemaFormat", Json.fromString(_)) ++
    maybeField(schemaEngine,"schemaEngine", Json.fromString(_)) ++
    maybeField(targetSchemaFormat,"targetSchemaFormat", Json.fromString(_)) ++
    maybeField(targetSchemaEngine,"targetSchemaEngine", Json.fromString(_)) ++
    maybeField(result,"result", Json.fromString(_) )
     //  maybeField(resultShapeMap,"shapeMap", (sm: ShapeMap) => sm.toJson)
  )
}

object SchemaConversionResult {
  def fromMsg(msg: String): SchemaConversionResult = SchemaConversionResult(msg,None,None,None,None,None,None,None)
  def fromConversion(source: String,
                     schemaFormat: String,
                     schemaEngine: String,
                     optTargetSchemaFormat: Option[String],
                     optTargetSchemaEngine: Option[String],
                     result: String,
                     resultShapeMap: ShapeMap
                    ): SchemaConversionResult =
    SchemaConversionResult(
      s"Conversion ($schemaFormat/$schemaEngine) => (${optTargetSchemaFormat.getOrElse("")}/${optTargetSchemaEngine.getOrElse("")}) successful",
      Some(source),
      Some(schemaFormat),
      Some(schemaEngine),
      optTargetSchemaFormat,
      optTargetSchemaEngine,
      Some(result),
      Some(resultShapeMap)
    )
}
