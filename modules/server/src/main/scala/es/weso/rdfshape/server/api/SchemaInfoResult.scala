package es.weso.rdfshape.server.api

import io.circe.Json

case class SchemaInfoResult(
    schema: String,
    schemaFormat: String,
    schemaEngine: String,
    shapes: Json,
    prefixMap: Json
)
