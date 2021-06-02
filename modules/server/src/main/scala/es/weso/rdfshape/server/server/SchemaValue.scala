package es.weso.rdfshape.server.server
import es.weso.rdfshape.server.server.format._
case class SchemaValue(
    schema: Option[String],
    schemaURL: Option[String],
    currentSchemaFormat: SchemaFormat,
    availableSchemaFormats: List[SchemaFormat],
    currentSchemaEngine: String,
    availableSchemaEngines: List[String],
    activeSchemaTab: String
)
