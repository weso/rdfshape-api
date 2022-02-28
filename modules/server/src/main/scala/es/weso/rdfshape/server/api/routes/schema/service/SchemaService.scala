package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.{
  ShExFormat,
  ShaclFormat
}
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.operations.{
  SchemaConvert,
  SchemaInfo,
  SchemaValidate
}
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.{
  TriggerMode,
  TriggerShapeMap
}
import es.weso.rdfshape.server.api.routes.schema.service.operations.SchemaConvertInput.decoder
import es.weso.rdfshape.server.api.routes.schema.service.operations.{
  SchemaConvertInput,
  SchemaInfoInput,
  SchemaValidateInput
}
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.implicits.string_parsers.instances.schemaEngineParser
import es.weso.schema.{
  JenaShacl,
  Schemas,
  ShExSchema,
  ShaclTQ,
  ShaclexSchema,
  Schema => SchemaW
}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes

/** API service to handle schema-related operations
  *
  * @param client HTTP4S client object
  */
class SchemaService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  override val verb: String = "schema"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    /** Returns a JSON array with the accepted schema engines a given schema type (ShEx or Shacl)
      */
    GET / `api` / `verb` / "engines" / pathVar[String] |>> { `type`: String =>
      val engines = `type`.toLowerCase match {
        case "shex" => List(Schemas.shEx)
        case "shacl" =>
          List(Schemas.shaclex, Schemas.jenaShacl, Schemas.shaclTQ)
      }
      val json = Json.fromValues(
        engines.map(_.name).map(str => Json.fromString(str))
      )
      Ok(json)
    }

    /** Returns the default schema engine as a raw string
      */
    GET / `api` / `verb` / "engines" / "default" |>> {
      val json = Json.fromString(Schemas.defaultSchema.name)
      Ok(json)
    }

    /** Returns the default schema format for a given engine as a raw string
      */
    GET / `api` / `verb` / "formats" / "default" / pathVar[SchemaW] |>> {
      engine: SchemaW =>
        val defaultFormat = engine match {
          case ShExSchema(_) => ShExFormat.default
          case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
            ShaclFormat.default
        }
        val json = Json.fromString(defaultFormat.name)
        Ok(json)
    }

    /** Returns a JSON array with the accepted schema formats.
      * Accepts an optional query parameter specifying the schema engine:
      * - schemaEngine [String]: schema engine for which we are listing the formats
      */
    GET / `api` / `verb` / "formats" / pathVar[SchemaW] |>> { engine: SchemaW =>
      val formats = engine match {
        case ShExSchema(_) => ShExFormat.availableFormats
        case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
          ShaclFormat.availableFormats
      }
      val json = Json.fromValues(formats.map(f => Json.fromString(f.name)))
      Ok(json)
    }

    /** Returns a JSON array with the accepted Trigger Modes
      */
    GET / `api` / `verb` / "triggerModes" |>> {
      val json = Json.fromValues(
        ApiDefinitions.availableTriggerModes.map(
          Json.fromString
        )
      )
      Ok(json)
    }

    /** Obtain information about an schema.
      * Receives a JSON object with the input schema information
      * Returns a JSON object with the operation results. See
      * [[SchemaInfo.encodeSchemaInfoOperation]].
      */
    POST / `api` / `verb` / "info" ^ jsonOf[IO, SchemaInfoInput] |>> {
      body: SchemaInfoInput =>
        SchemaInfo
          .schemaInfo(body.schema)
          .flatMap(info => Ok(info.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))

    }

    /** Convert a given schema to another accepted format (this includes
      * graphic formats for visualizations).
      * Receives a JSON object with the input schema information
      * Returns a JSON object with the operation results. See
      * [[SchemaConvert.encodeSchemaConvertOperation]].
      */
    POST / `api` / `verb` / "convert" ^ jsonOf[IO, SchemaConvertInput] |>> {
      body: SchemaConvertInput =>
        SchemaConvert
          .schemaConvert(
            body.schema,
            body.targetFormat,
            body.targetEngine
          )
          .flatMap(result => Ok(result.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
    }

    /** Validates RDF data against a given schema-shapemap.
      * Receives a JSON object with the input data, schema and shapemap
      * information
      * Returns a JSON object with the operation results. See
      * [[SchemaValidate.encodeSchemaValidateOperation]]
      * @note When obtaining the trigger mode from the parameters,
      *       if the [[TriggerMode]] is shapeMap, the corresponding [[ShapeMap]]
      *       object will be embedded in the resulting [[TriggerShapeMap]]
      */
    POST / `api` / `verb` / "validate" ^ jsonOf[IO, SchemaValidateInput] |>> {
      body: SchemaValidateInput =>
        SchemaValidate
          .schemaValidate(body.data, body.schema, body.triggerMode)
          .flatMap(result => Ok(result.asJson))
          .handleErrorWith(err => InternalServerError(err.getMessage))
    }
  }

}

object SchemaService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Schema Service
    */
  def apply(client: Client[IO]): SchemaService =
    new SchemaService(client)
}

private object SchemaServiceError extends Enumeration {
  type SchemaServiceError = String
  val couldNotValidateData: SchemaServiceError =
    "Unknown error validating the data provided. Check the inputs."
}
