package es.weso.rdfshape.server.api.routes.schema.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.ApiHelper.umlOptions
import es.weso.schema.Schema
import es.weso.uml.Schema2UML
import io.circe.Json

/** Static utilities used by the {@link es.weso.rdfshape.server.api.routes.schema.service.SchemaService}
  * to operate on schemas
  */
private[schema] object SchemaOperations extends LazyLogging {

  /** Obtain the information from an schema
    *
    * @param schema Input schema
    * @return Schema information as a data instance of {@link SchemaInfo}.
    */
  def schemaInfo(schema: Schema): SchemaInfo = {
    val info = schema.info
    SchemaInfo(
      Some(info.schemaName),
      Some(info.schemaEngine),
      info.isWellFormed,
      schema.shapes,
      schema.pm.pm.toList.map { case (prefix, iri) => (prefix.str, iri.str) },
      info.errors
    )
  }

  /** @param schema Input schema
    * @return JSON representation of the schema as a Cytoscape graph to be drawn on clients (or an error message)
    */
  // TODO: return another status code on failure, so that clients can handle it
  def schemaCytoscape(schema: Schema): Json = {
    val eitherJson = for {
      pair <- Schema2UML.schema2UML(schema)
    } yield {
      val (uml, warnings) = pair
      uml.toJson
    }
    eitherJson.fold(
      e =>
        Json.fromFields(
          List(
            ("error", Json.fromString(s"Error converting to schema 2 JSON: $e"))
          )
        ),
      identity
    )
  }

  /** @param schema Input schema
    * @return JSON representation of the schema as a Graphviz graph to be drawn on clients (or an error message)
    */
  // TODO: return another status code on failure, so that clients can handle it
  def schemaVisualize(schema: Schema): IO[Json] = for {
    pair <- schema2SVG(schema)
  } yield {
    val (svg, plantuml) = pair
    val info            = schema.info
    val fields: List[(String, Json)] =
      List(
        ("schemaName", Json.fromString(info.schemaName)),
        ("schemaEngine", Json.fromString(info.schemaEngine)),
        ("wellFormed", Json.fromBoolean(info.isWellFormed)),
        ("errors", Json.fromValues(info.errors.map(Json.fromString))),
        ("parsed", Json.fromString("Parsed OK")),
        ("svg", Json.fromString(svg)),
        ("plantUML", Json.fromString(plantuml))
      )
    Json.fromFields(fields)
  }

  def schema2SVG(schema: Schema): IO[(String, String)] = {
    val eitherUML = Schema2UML.schema2UML(schema)
    eitherUML.fold(
      e => IO.pure((s"SVG conversion: $e", s"Error converting UML: $e")),
      pair => {
        val (uml, warnings) = pair
        logger.debug(s"UML converted: $uml")
        (for {
          str <- uml.toSVG(umlOptions)
        } yield {
          (str, uml.toPlantUML(umlOptions))
        }).handleErrorWith(e =>
          IO.pure(
            (
              s"SVG conversion error: ${e.getMessage}",
              uml.toPlantUML(umlOptions)
            )
          )
        )
      }
    )
  }

}
