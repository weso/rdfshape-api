package es.weso.rdfshape.server.api.routes.schema.logic.aux

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.{InferenceEngine, RDFReasoner}
import es.weso.rdfshape.server.api.definitions.UmlDefinitions.umlOptions
import es.weso.rdfshape.server.api.routes.schema.service.SchemaService
import es.weso.schema.Schema
import es.weso.uml.Schema2UML
import io.circe.Json

/** Static utilities used by [[SchemaService]]
  * to operate on schemas
  */
private[api] object SchemaOperations extends LazyLogging {

  /** Long value used as a "no time" value for errored validations
    */
  private val NoTime = 0L

  /** @param schema Input schema
    * @return JSON representation of the schema as a Cytoscape graph to be drawn on clients (or an error message)
    */
  // TODO: return another status code on failure, so that clients can handle it
  def schemaCytoscape(schema: Schema): Json = {
    val eitherJson = for {
      pair <- Schema2UML.schema2UML(schema)
    } yield {
      val (uml, _) = pair
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
        ("schemaType", Json.fromString(info.schemaName)),
        ("schemaEngine", Json.fromString(info.schemaEngine)),
        ("svg", Json.fromString(svg)),
        ("plantUml", Json.fromString(plantuml))
      )
    Json.fromFields(fields)
  }

  def schema2SVG(schema: Schema): IO[(String, String)] = {
    val eitherUML = Schema2UML.schema2UML(schema)
    eitherUML.fold(
      err => {
        val errMsg = s"Error in SVG conversion: $err"
        logger.error(errMsg)
        IO.raiseError(new RuntimeException(errMsg))
        //        IO.pure((s"SVG conversion: $e", s"Error converting UML: $e"))
      },
      pair => {
        val (uml, _) = pair
        logger.debug(s"UML converted: $uml")
        (for {
          str <- uml.toSVG(umlOptions)
        } yield {
          (str, uml.toPlantUML(umlOptions))
        }).handleErrorWith(e =>
          IO.raiseError(
            new RuntimeException(s"SVG conversion error: ${e.getMessage}")
          )
        )
      }
    )
  }

  /** Apply inference
    *
    * @param rdf             Data over which the inference should be applied
    * @param inferenceEngine Inference engine to be applied
    * @return The RDF data after applying the inference
    *         (or the intact data if no inference was provided)
    */
  private[schema] def applyInference(
      rdf: RDFReasoner,
      inferenceEngine: Option[InferenceEngine]
  ): IO[RDFReasoner] = inferenceEngine match {
    case None => IO.pure(rdf)
    case Some(engine) =>
      rdf.applyInference(engine)
  }
}
