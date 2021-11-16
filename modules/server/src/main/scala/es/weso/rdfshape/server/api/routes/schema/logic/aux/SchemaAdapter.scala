package es.weso.rdfshape.server.api.routes.schema.logic.aux

import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.schema.{Schemas, Schema => SchemaW}
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Adapter, codecs and utils between the server's Schema class ([[Schema]])
  * and shaclex Schema engine types ([[SchemaW]])
  */
private[schema] object SchemaAdapter {

  /** For a given schema engine name, try to map it to the schema it represents
    *
    * @param engineName Name (String) of the given schema engine
    * @return The schema engine corresponding to the given name, if available
    */
  def schemaEngineFromString(engineName: String): Option[SchemaW] = {
    Schemas.availableSchemas
      .find(schema => schema.name.toLowerCase == engineName.toLowerCase())
  }

  /** Simple encoder for [[SchemaW]] instances, simplifying them to their name
    */
  implicit val encodeEngine: Encoder[SchemaW] = (schemaEngine: SchemaW) =>
    Json.fromString(schemaEngine.name)

  /** Auxiliary decoder for data inference
    */
  implicit val decodeEngine: Decoder[SchemaW] =
    (cursor: HCursor) =>
      for {
        engineName <- cursor.value.as[String]

        engine = Schemas.availableSchemas
          .find(
            _.name.toLowerCase == engineName.toLowerCase
          )
          .getOrElse(ApiDefaults.defaultSchemaEngine)
      } yield engine
}
