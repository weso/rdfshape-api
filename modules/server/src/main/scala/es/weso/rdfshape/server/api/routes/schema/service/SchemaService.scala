package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.{
  ShExFormat,
  ShaclFormat
}
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.aux.SchemaAdapter
import es.weso.rdfshape.server.api.routes.schema.logic.operations.{
  SchemaConvert,
  SchemaInfo,
  SchemaValidate
}
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.{
  TriggerMode,
  TriggerModeType,
  TriggerShapeMap
}
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.schema.{Schemas, ShExSchema, Result => ValidationResult}
import io.circe.Json
import io.circe.syntax.EncoderOps
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
      val engineNames = Schemas.availableSchemaNames
      val json        = Json.fromValues(engineNames.map(Json.fromString))
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
      val maybeFormats = for {
        schema <- Schemas.lookupSchema(
          optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
        )
        formats = schema match {
          case ShExSchema(_) => ShExFormat.availableFormats
          case _             => ShaclFormat.availableFormats
        }
      } yield Json.fromValues(
        formats.map(format => Json.fromString(format.name))
      )

      // Handle errors
      maybeFormats
        .flatMap(Ok(_))
        .handleErrorWith(err =>
          errorResponseJson(err.getMessage, InternalServerError)
        )

    /** Returns a JSON array with the accepted Trigger Modes
      */
    case GET -> Root / `api` / `verb` / "triggerModes" =>
      val json = Json.fromValues(
        List(TriggerModeType.SHAPEMAP, TriggerModeType.TARGET_DECLARATIONS).map(
          Json.fromString
        )
      )
      Ok(json)

    /** Obtain information about an schema.
      * Receives a JSON object with the input schema information:
      *  - schema [String]: Schema data (raw, URL containing the schema or File with the schema)
      *  - schemaSource [String]: Identifies the source of the schema (raw, URL, file...)
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema (ignored for ShEx)
      *    Returns a JSON object with the operation results. See [[SchemaInfo.encodeSchemaInfoOperation]].
      */
    case req @ POST -> Root / `api` / `verb` / "info" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)

          for {
            // Get the schema from the partsMap
            eitherSchema <- Schema.mkSchema(partsMap)
            response <- eitherSchema.fold(
              // If there was an error parsing the schema, return it
              err => errorResponseJson(err, InternalServerError),
              // Else, try and compute the schema info
              schema =>
                SchemaInfo
                  .schemaInfo(schema)
                  .flatMap(info => Ok(info.asJson))
                  .handleErrorWith(err =>
                    errorResponseJson(err.getMessage, InternalServerError)
                  )
            )

          } yield response
        }
      }

    /** Convert a given schema to another accepted format (this includes
      * graphic formats for visualizations).
      * * Receives a JSON object with the input schema information:
      *  - schema [String]: Schema data (raw, URL containing the schema or File with the schema)
      *  - schemaSource [String]: Identifies the source of the schema (raw, URL, file...)
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema (ignored for ShEx)
      *  - targetSchemaFormat [String]: Desired format after conversion of the schema
      *    Returns a JSON object with the operation results. See [[SchemaConvert.encodeSchemaConvertOperation]].
      */
    case req @ POST -> Root / `api` / `verb` / "convert" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          for {
            // Get the schema from the partsMap
            eitherSchema <- Schema.mkSchema(partsMap)
            // Get the target schema format
            optTargetFormatStr <- partsMap.optPartValue(
              TargetSchemaFormatParameter.name
            )
            optTargetFormat = for {
              targetFormatStr <- optTargetFormatStr
              targetFormat <- DataFormat
                .fromString(targetFormatStr)
                .toOption
            } yield targetFormat

            // Get the target engine
            optTargetEngineStr <- partsMap.optPartValue(
              TargetSchemaEngineParameter.name
            )
            optTargetEngine = for {
              targetEngineStr <- optTargetEngineStr
              targetEngine <- SchemaAdapter.schemaEngineFromString(
                targetEngineStr
              )
            } yield targetEngine

            // Abort if no valid target format, else continue
            response <- optTargetFormat match {
              case None =>
                errorResponseJson(
                  "Empty or invalid target format for conversion",
                  BadRequest
                )
              case Some(targetFormat) =>
                eitherSchema match {
                  case Left(err) => errorResponseJson(err, InternalServerError)
                  case Right(schema) =>
                    SchemaConvert
                      .schemaConvert(
                        schema,
                        targetFormat,
                        optTargetEngine
                      )
                      .flatMap(result => Ok(result.asJson))
                      .handleErrorWith(err =>
                        errorResponseJson(err.getMessage, InternalServerError)
                      )
                }
            }
          } yield response
        }
      }

    /** Validates RDF data against a given schema-shapemap.
      * Receives a JSON object with the input data, schema and shapemap information:
      *
      *  - data [String]: RDF data (raw, URL containing the data or File with the data)
      *  - dataSource [String]: Identifies the source of the data (raw, URL, file...) so that the server knows how to handle it
      *  - dataFormat [String]: Format of the RDF data
      *  - inference [String]: Inference to be applied to the data
      *
      *  - schema [String]: Schema data (raw, URL containing the schema or File with the schema)
      *  - schemaSource [String]: Identifies the source of the schema (raw, URL, file...)
      *  - schemaFormat [String]: Format of the schema
      *  - schemaEngine [String]: Engine used to process the schema (ignored for ShEx)
      *
      *  - shapeMap [String]: ShapeMap data (raw, URL containing the shapeMap or File with the shapeMap)
      *  - shapeMapSource [String]: Identifies the source of the shapeMap (raw, URL, file...)
      *  - shapeMapFormat [String]: Format of the shapemap
      *
      *  - endpoint [String]: Additional endpoint to serve as a source of data
      *  - triggerMode [String]: Validation trigger mode
      *
      * Returns a JSON object with the operation results. See [[SchemaValidate.encodeSchemaValidateOperation]] and [[ValidationResult.toJson()]].
      *
      * @note When obtaining the trigger mode from the parameters,
      *       if the [[TriggerMode]] is shapeMap, the corresponding [[ShapeMap]]
      *       object will be embedded in the resulting [[TriggerShapeMap]]
      */
    case req @ POST -> Root / `api` / `verb` / "validate" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val response = for {
          /* Get the data, schema and trigger-mode from the partsMap.
           * If the trigger-mode is "shapeMap", the shapemap will be embedded in
           * the trigger object */

          eitherData <- Data.mkData(partsMap)

          eitherSchema <- Schema.mkSchema(partsMap)

          eitherTriggerMode <- TriggerMode
            .mkTriggerMode(
              partsMap,
              eitherData.toOption,
              eitherSchema.toOption
            )

          // Contains either the first error encountered or the validation
          eitherValidationData = for {
            data    <- eitherData
            schema  <- eitherSchema
            trigger <- eitherTriggerMode
          } yield (data, schema, trigger)

          response <- eitherValidationData match {
            case Left(err) => errorResponseJson(err, InternalServerError)
            case Right((data, schema, trigger)) =>
              SchemaValidate
                .schemaValidate(data, schema, trigger)
                .flatMap(result => Ok(result.asJson))
                .handleErrorWith(err =>
                  errorResponseJson(err.getMessage, InternalServerError)
                )
          }

        } yield response
        response.handleErrorWith(err =>
          err.getMessage match {
            case msg: String =>
              errorResponseJson(
                msg,
                InternalServerError
              )
            case _ =>
              errorResponseJson(
                SchemaServiceError.couldNotValidateData,
                InternalServerError
              )
          }
        )

      //        {
      //          val partsMap = PartsMap(m.parts)
      //          val r = for {
      //            dataPair <- DataSingle.getData(partsMap, relativeBase)
      //            (resourceRdf, dp) = dataPair
      //            res <- for {
      //              emptyRes <- RDFAsJenaModel.empty
      // vv <- (resourceRdf, emptyRes).tupled.use { case (rdf, builder) =>
      //                for {
      //                  schemaPair <- Schema.mkSchema(partsMap, Some(rdf))
      //                  (schema, _) = schemaPair
      /* maybeTriggerMode <- TriggerMode.mkTriggerMode(partsMap) */
      /* newRdf <- applyInference(rdf, dp.inference) */
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
