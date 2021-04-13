package es.weso.server
import es.weso.server.format._
case class SchemaValue(
    schema: Option[String],
    schemaURL: Option[String],
    currentSchemaFormat: SchemaFormat,
    availableSchemaFormats: List[SchemaFormat],
    currentSchemaEngine: String,
    availableSchemaEngines: List[String],
    activeSchemaTab: String
)
