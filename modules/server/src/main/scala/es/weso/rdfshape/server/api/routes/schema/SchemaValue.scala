package es.weso.rdfshape.server.api.routes.schema

import es.weso.rdfshape.server.api.format.SchemaFormat

case class SchemaValue(
    schema: Option[String],
    schemaURL: Option[String],
    currentSchemaFormat: SchemaFormat,
    availableSchemaFormats: List[SchemaFormat],
    currentSchemaEngine: String,
    availableSchemaEngines: List[String],
    activeSchemaTab: String
)
