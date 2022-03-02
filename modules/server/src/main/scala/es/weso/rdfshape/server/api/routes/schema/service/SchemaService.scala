package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
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
import es.weso.rdfshape.server.api.routes.schema.service.operations.SchemaConvertInput.decoder
import es.weso.rdfshape.server.api.routes.schema.service.operations.{
  SchemaConvertInput,
  SchemaInfoInput,
  SchemaValidateInput
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.SchemaEngineParameter
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
import org.http4s.rho.swagger.syntax.io._

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

    s"""Get an array with the accepted schema engines 
       | given an schema type (${SchemaServiceDescriptions.SchemaType.schemaTypes
      .mkString(" or ")})""".stripMargin **
      GET / `verb` / "engines" / pathVar[String](
        SchemaServiceDescriptions.SchemaType.name,
        SchemaServiceDescriptions.SchemaType.description
      ) |>> { `type`: String =>
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

    "Get the default schema engine as a raw string" **
      GET / `verb` / "engines" / "default" |>> {
        val json = Json.fromString(Schemas.defaultSchema.name)
        Ok(Schemas.defaultSchema.name)
      }

    "Get the default schema format for a given schema engine as a raw string" **
      GET / `verb` / "formats" / "default" / pathVar[SchemaW](
        SchemaServiceDescriptions.SchemaEngine.name,
        SchemaServiceDescriptions.SchemaEngine.description
      ) |>> { engine: SchemaW =>
        val defaultFormat = engine match {
          case ShExSchema(_) => ShExFormat.default
          case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
            ShaclFormat.default
        }
        Ok(defaultFormat.name)
      }

    "Get an array with the accepted schema formats for a given schema engine" **
      GET / `verb` / "formats" / pathVar[SchemaW](
        SchemaServiceDescriptions.SchemaEngine.name,
        SchemaServiceDescriptions.SchemaEngine.description
      ) |>> { engine: SchemaW =>
        val formats = engine match {
          case ShExSchema(_) => ShExFormat.availableFormats
          case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
            ShaclFormat.availableFormats
        }
        val json = Json.fromValues(formats.map(f => Json.fromString(f.name)))
        Ok(json)
      }

    "Get an array with the accepted Trigger Modes for validations" **
      GET / `verb` / "triggerModes" |>> {
        val json = Json.fromValues(
          ApiDefinitions.availableTriggerModes.map(
            Json.fromString
          )
        )
        Ok(json)
      }

    "Obtain information about an schema: list of shapes and prefix map" **
      POST / `verb` / "info" ^ jsonOf[IO, SchemaInfoInput] |>> {
        body: SchemaInfoInput =>
          SchemaInfo
            .schemaInfo(body.schema)
            .flatMap(info => Ok(info.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))

      }

    "Convert a given schema to another format (this includes graphic formats for visualizations)" **
      POST / `verb` / "convert" ^ jsonOf[IO, SchemaConvertInput] |>> {
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

    "Validates RDF data against a given schema" **
      POST / `verb` / "validate" ^ jsonOf[IO, SchemaValidateInput] |>> {
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

/** Compendium of additional text constants used on service errors
  */
private object SchemaServiceError {
  val couldNotValidateData =
    "Unknown error validating the data provided. Check the inputs."
}

/** Compendium of additional text constants used to describe inline parameters
  * (query and path parameters) in Swagger
  */
private object SchemaServiceDescriptions {
  case object SchemaEngine {
    val name: String = SchemaEngineParameter.name
    val description =
      s"Engine in which the validation schema is redacted. One of: ${ApiDefinitions.availableSchemaEngines.map(_.name).mkString(", ")}"
  }

  case object SchemaType {
    val schemaTypes = List("ShEx", "SHACL")
    val name        = "SchemaType"
    val description =
      s"Type of the validation schema. One of: ${schemaTypes.mkString(", ")}"
  }
}
