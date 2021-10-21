package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.definitions.ApiDefaults.defaultSchemaEngineName
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.format.dataFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.Schema
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaOperations._
import es.weso.rdfshape.server.api.utils.OptEitherF._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.schema._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart

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
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    /** Returns a JSON array with the accepted schema engines for ShEx
      */
    case GET -> Root / `api` / `verb` / "engines" =>
      val engines = Schemas.availableSchemaNames
      val json    = Json.fromValues(engines.map(str => Json.fromString(str)))
      Ok(json)

    /** Returns a JSON array with the accepted schema engines for SHACL
      */
    case GET -> Root / `api` / `verb` / "engines" / "shacl" =>
      val shaclSchemas =
        List(Schemas.shaclex, Schemas.jenaShacl, Schemas.shaclTQ)
      val json = Json.fromValues(
        shaclSchemas.map(_.name).map(str => Json.fromString(str))
      )
      Ok(json)

    /** Returns the default schema format as a raw string
      */
    case GET -> Root / `api` / `verb` / "engines" / "default" =>
      val schemaEngine = Schemas.defaultSchemaName
      val json         = Json.fromString(schemaEngine)
      Ok(json)

    /** Returns a JSON array with the accepted schema formats.
      * Accepts an optional query parameter specifying the schema engine:
      * - schemaEngine [String]: schema engine for which we are listing the formats
      */
    case GET -> Root / `api` / `verb` / "formats" :?
        SchemaEngineParameter(optSchemaEngine) =>
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val res = Schemas
        .lookupSchema(schemaEngine)
        .attempt
        .map(
          _.fold(
            _ =>
              errorResponseJson(
                s"Schema engine: $schemaEngine not found. Available engines = ${Schemas.availableSchemaNames
                  .mkString(",")}",
                NotFound
              ),
            schema =>
              Ok(Json.fromValues(schema.formats.toList.map(Json.fromString)))
          )
        )
      res.flatten

    /** Returns a JSON array with the accepted triggerModes
      */
    case GET -> Root / `api` / `verb` / "triggerModes" =>
      val triggerModes = ValidationTrigger.triggerValues.map(_._1)
      val json         = Json.fromValues(triggerModes.map(Json.fromString))
      Ok(json)

    /** Obtain information about an schema.
      * Receives a JSON object with the input schema information:
      *  - schema [String]: Raw schema data
      *  - schemaUrl [String]: Url containing the schema
      *  - schemaFile [File Object]: File containing schema
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema
      *  - activeSchemaTab [String]: Identifies the source of the schema (raw, URL, file...)
      *    Returns a JSON object with the schema information:
      *    - schemaType [String]: Type of the schema
      *    - schemaEngine [String]: Engine of the schema
      *    - wellFormed [Boolean]: Whether if the schema is well formed or not
      *    - shapes [Array]: Array of the shapes in the schema
      *    - shapesPrefixMap [Array]: Array of the prefixes in the schema
      *        - prefix [String]: Prefix key
      *        - uri [String]: Prefix URI
      *    - errors [Array]: Array of errors in the schema
      */
    // TODO: show errors in a friendlier way in the client's UI
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- Schema.mkSchema(partsMap, None)
            (schema, sp) = schemaPair
          } yield {
            schemaInfo(schema).toJson
          }
          for {
            e <- r.attempt
            v <- e.fold(
              t => {
                errorResponseJson(
                  t.getMessage,
                  BadRequest
                )
              },
              Ok(_)
            )
          } yield v
        }
      }

    /** Convert a given schema to another accepted format.
      * Receives a JSON object with the input schema information:
      *  - schema [String]: Raw schema data
      *  - schemaUrl [String]: Url containing the schema
      *  - schemaFile [File Object]: File containing schema
      *  - schemaFormat [String]: Format of the schema
      *  - targetSchemaFormat [String]: Desired format after conversion of the schema
      *  - schemaEngine [String]: Engine used to process the schema
      *  - activeSchemaTab [String]: Identifies the source of the schema (raw, URL, file...)
      *    Returns a JSON object with the converted schema information:
      *    - message [String]: Informational message on success
      *    - schema [String]: Original input schema
      *    - schemaFormat [String]: Format of the original schema
      *    - schemaEngine [String]: Engine of the conversion
      *    - targetSchemaFormat [String]: Format of the output schema
      *    - result [String]: Output schema
      *    - shapeMap [String]: Output shapemap, if any
      */
    case req @ POST -> Root / `api` / `verb` / "convert" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- Schema.mkSchema(partsMap, None)
            (schema, sp) = schemaPair

            targetSchemaFormat <- optEither2f(
              sp.targetSchemaFormat,
              SchemaFormat.fromString
            )
            converted <- convertSchema(
              schema,
              sp.schema,
              sp.schemaFormat,
              sp.schemaEngine.getOrElse(defaultSchemaEngineName),
              targetSchemaFormat,
              sp.targetSchemaEngine
            )
          } yield {
            converted.toJson
          }
          for {
            e <- r.attempt
            v <- e.fold(
              t => errorResponseJson(t.getMessage, InternalServerError),
              Ok(_)
            )
          } yield v
        }
      }

    /** Convert a given schema to a UML visualization using PlantUML.
      * Receives a JSON object with the input schema information:
      *  - schema [String]: Raw schema data
      *  - schemaUrl [String]: Url containing the schema
      *  - schemaFile [File Object]: File containing schema
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema
      *  - activeSchemaTab [String]: Identifies the source of the schema (raw, URL, file...)
      *    Returns a JSON object with the converted schema information:
      *    - schemaType [String]: Type of the schema
      *    - schemaEngine [String]: Engine of the schema
      *    - svg [String]: Array of the shapes in the schema
      *    - plantUml [String]: Array of the shapes in the schema
      */
    case req @ POST -> Root / `api` / `verb` / "visualize" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          val r: IO[Json] = for {
            schemaPair <- Schema.mkSchema(partsMap, None)
            (schema, _) = schemaPair
            v <- schemaVisualize(schema)
          } yield {
            v
          }
          for {
            e <- r.attempt
            v <- e.fold(t => errorResponseJson(t.getMessage, BadRequest), Ok(_))
          } yield v
        }
      }

    // TODO: test and include in the client
    case req @ POST -> Root / `api` / `verb` / "cytoscape" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- Schema.mkSchema(partsMap, None)
            (schema, _) = schemaPair
          } yield {
            schemaCytoscape(schema)
          }
          for {
            e <- r.attempt
            v <- e.fold(t => errorResponseJson(t.getMessage, BadRequest), Ok(_))
          } yield v
        }
      }

    // TODO: Enhance API response
    /** Validates RDF data against a given schema-shapemap.
      * Receives a JSON object with the input data, schema and shapemap information:
      *  - data [String]: RDF data
      *  - dataUrl [String]: Url containing the RDF data
      *  - dataFile [File Object]: File containing RDF data
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied
      *  - activeDataTab [String]: Identifies the source of the data (raw, URL, file...)
      *  - endpoint [String]: Additional endpoint to serve as a source of data
      *  - schema [String]: Raw schema data
      *  - schemaUrl [String]: Url containing the schema
      *  - schemaFile [File Object]: File containing the schema
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema
      *  - activeSchemaTab [String]: Identifies the source of the schema (raw, URL, file...)
      *  - triggerMode [String]: Validation trigger mode
      *  - shapeMap [String]: Raw shapemap data
      *  - shapeMapUrl [String]: Url containing the shapemap
      *  - shapeMapFile [File Object]: File containing the shapemap
      *  - shapeMapFormat [String]: Format of the shapemap
      *  - activeShapeMapTab [String]: Identifies the source of the shapemap (raw, URL, file...)
      *    Returns a JSON object with the converted schema information:
      *    - valid [Boolean]: Whether the data is at least partially valid or not
      *    - message [String]: Informational message
      *    - validationReport [String]: Additional validation information
      *    - schema [String]: Original input schema
      *    - nodesPrefixMap [Object]: Key/value structure with the data prefixes
      *    - shapesPrefixMap [Object]: Key/value structure with the schema prefixes
      *    - shapeMap [Array]: Array containing the validation results for each node. Each result has:
      *        - node [String]: Full name of the affected node
      *        - shape [String]: Full name of the affected shape
      *        - status [String]: Whether this node conforms this shape
      *        - appInfo [Object]: Additional information on why the node conforms or not
      *    - errors [Array]: Array of errors in the validation
      */
    /* TODO: redo */
    //    case req @ POST -> Root / `api` / `verb` / "validate" =>
    //      req.decode[Multipart[IO]] { m =>
    //        {
    //          val partsMap = PartsMap(m.parts)
    //          val r = for {
    //            dataPair <- DataSingle.getData(partsMap, relativeBase)
    //            (resourceRdf, dp) = dataPair
    //            res <- for {
    //              emptyRes <- RDFAsJenaModel.empty
    /* vv <- (resourceRdf, emptyRes).tupled.use { case (rdf, builder) => */
    //                for {
    //                  schemaPair <- Schema.mkSchema(partsMap, Some(rdf))
    //                  (schema, _) = schemaPair
    //                  maybeTriggerMode <- TriggerMode.mkTriggerMode(partsMap)
    //                  newRdf           <- applyInference(rdf, dp.inference)
    //                  ret <- maybeTriggerMode match {
    //                    case Left(err) =>
    //                      IO.raiseError(
    //                        new RuntimeException(
    //                          s"Could not obtain validation trigger: $err"
    //                        )
    //                      )
    //                    case Right(triggerMode) =>
    //                      for {
    //                        r <- io2f(
    //                          schemaValidate(
    //                            newRdf,
    //                            schema,
    //                            triggerMode,
    //                            relativeBase,
    //                            builder
    //                          )
    //                        )
    //                        json <- io2f(schemaResult2json(r._1))
    //                      } yield json
    //                  }
    //                } yield ret
    //              }
    //            } yield vv
    //          } yield res
    //
    //          for {
    //            e <- r.attempt
    //            res <- e.fold(
    //              exc => errorResponseJson(exc.getMessage, BadRequest),
    //              json => Ok(json)
    //            )
    //          } yield res
    //        }
    //      }
  }
  private val relativeBase = ApiDefaults.relativeBase

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
