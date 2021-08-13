package es.weso.rdfshape.server.api.routes.schema

import io.circe.Json

case class SchemaInfoResult(
    schema: String,
    schemaFormat: String,
    schemaEngine: String,
    shapes: Json,
    prefixMap: Json
)
