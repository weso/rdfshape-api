package es.weso.rdfshape.server.api.routes.schema.service

import cats.data._
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.{InferenceEngine, RDFReasoner}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.results._
import es.weso.rdfshape.server.api.routes.ApiDefinitions._
import es.weso.rdfshape.server.api.routes.ApiHelper._
import es.weso.rdfshape.server.api.routes.Defaults._
import es.weso.rdfshape.server.api.routes.IncomingRequestParameters._
import es.weso.rdfshape.server.api.routes.data.DataParam
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaOperations.{
  schema2SVG,
  schemaCytoscape,
  schemaInfo,
  schemaVisualize
}
import es.weso.rdfshape.server.api.routes.schema.logic.{
  SchemaInfo,
  SchemaInfoResult
}
import es.weso.rdfshape.server.api.routes.{Defaults, PartsMap}
import es.weso.rdfshape.server.api.utils.OptEitherF._
import es.weso.rdfshape.server.utils.json.JsonUtils.responseJson
import es.weso.schema._
import es.weso.shacl.converter.Shacl2ShEx
import es.weso.shapemaps.ShapeMap
import es.weso.utils.IOUtils._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart

/** API service to handle schema-related operations
  *
  * @param client HTTP4S client object
  */
class SchemaService(client: Client[IO]) extends Http4sDsl[IO] with LazyLogging {

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "schema" / "engines" =>
      val engines = Schemas.availableSchemaNames
      val json    = Json.fromValues(engines.map(str => Json.fromString(str)))
      Ok(json)

    case GET -> Root / `api` / "schema" / "engines" / "shacl" =>
      val shaclSchemas =
        List(Schemas.shaclex, Schemas.jenaShacl, Schemas.shaclTQ)
      val json = Json.fromValues(
        shaclSchemas.map(_.name).map(str => Json.fromString(str))
      )
      Ok(json)

    case GET -> Root / `api` / "schema" / "engines" / "default" =>
      val schemaEngine = Schemas.defaultSchemaName
      val json         = Json.fromString(schemaEngine)
      Ok(json)

    case GET -> Root / `api` / "schema" / "formats" :?
        SchemaEngineParam(optSchemaEngine) =>
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val r: IO[Json] = Schemas
        .lookupSchema(schemaEngine)
        .attempt
        .map(
          _.fold(
            err =>
              Json.fromFields(
                List(
                  (
                    "error",
                    Json.fromString(
                      s"Schema engine: $schemaEngine not found. Available engines = ${Schemas.availableSchemaNames
                        .mkString(",")}"
                    )
                  )
                )
              ),
            schema =>
              Json.fromValues(schema.formats.toList.map(Json.fromString))
          )
        )
      io2f(r).flatMap(json => Ok(json))

    case GET -> Root / `api` / "schema" / "triggerModes" =>
      val triggerModes = ValidationTrigger.triggerValues.map(_._1)
      val json         = Json.fromValues(triggerModes.map(Json.fromString))
      Ok(json)

    case GET -> Root / `api` / "schema" / "info" :?
        OptSchemaParam(optSchema) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) =>
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val schemaStr = optSchema match {
        case None         => ""
        case Some(schema) => schema
      }
      for {
        either <- Schemas
          .fromString(schemaStr, schemaFormat, schemaEngine, None)
          .attempt
        r <- either.fold(
          e => responseJson(s"Error reading schema: $e\nString: $schemaStr"),
          schema => {
            val shapes: List[String] = schema.shapes
            val jsonShapes           = Json.fromValues(shapes.map(Json.fromString))
            val pm: Json             = prefixMap2Json(schema.pm)
            val result = SchemaInfoResult(
              schemaStr,
              schemaFormat,
              schemaEngine,
              jsonShapes,
              pm
            ).asJson
            Ok(result)
          }
        )
      } yield r

    case req @ POST -> Root / `api` / "schema" / "info" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap, None)
            (schema, sp) = schemaPair
          } yield {
            schemaInfo(schema).toJson
          }
          for {
            e <- r.attempt
            v <- e.fold(
              t => {
                Ok(SchemaInfo.fromError(t.getMessage).toJson)
              },
              Ok(_)
            )
          } yield v
        }
      }

    case GET -> Root / `api` / "schema" / "convert" :?
        OptSchemaParam(optSchema) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) +&
        TargetSchemaFormatParam(optResultSchemaFormat) +&
        TargetSchemaEngineParam(optResultSchemaEngine) =>
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val schemaStr = optSchema match {
        case None         => ""
        case Some(schema) => schema
      }
      for {
        maybeSchemaFormat <- optEither2f(
          optSchemaFormat,
          SchemaFormat.fromString
        )
        schemaFormat = maybeSchemaFormat.getOrElse(defaultSchemaFormat)
        either <- Schemas
          .fromString(schemaStr, schemaFormat.name, schemaEngine, None)
          .attempt
        r <- either.fold(
          e => responseJson(s"Error reading schema: $e\nString: $schemaStr"),
          schema => {
            for {
              optTargetSchemaFormat <- optEither2f(
                optResultSchemaFormat,
                SchemaFormat.fromString
              )
              s <- io2f(
                convertSchema(
                  schema,
                  optSchema,
                  schemaFormat,
                  schemaEngine,
                  optTargetSchemaFormat,
                  optResultSchemaEngine
                )
              )
              r <- Ok(s.toJson)
            } yield r
          }
          /* Ok(convertSchema(schema, optSchema, schemaFormat, schemaEngine,
           * optResultSchemaFormat, optResultSchemaEngine).toJson) */
        )
      } yield r

    case req @ POST -> Root / `api` / "schema" / "convert" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap, None)
            (schema, sp) = schemaPair
            /* targetSchemaFormat <- optEither2f(sp.targetSchemaFormat,
             * SchemaFormat.fromString) */
            targetSchemaFormat <- optEither2f(
              sp.targetSchemaFormat,
              SchemaFormat.fromString
            )
            converted <- convertSchema(
              schema,
              sp.schema,
              sp.schemaFormat.getOrElse(SchemaFormat.defaultFormat),
              sp.schemaEngine.getOrElse(defaultSchemaEngine),
              targetSchemaFormat,
              sp.targetSchemaEngine
            )
          } yield {
            converted.toJson
          }
          for {
            e <- r.attempt
            v <- e.fold(
              t => Ok(SchemaConversionResult.fromMsg(t.getMessage).toJson),
              Ok(_)
            )
          } yield v
        }
      }

    case req @ POST -> Root / `api` / "schema" / "visualize" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap, None)
            (schema, _) = schemaPair
            v <- schemaVisualize(schema)
          } yield {
            v
          }
          for {
            e <- r.attempt
            v <- e.fold(t => responseJson(t.getMessage), Ok(_))
          } yield v
        }
      }

    case req @ POST -> Root / `api` / "schema" / "cytoscape" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: IO[Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap, None)
            (schema, _) = schemaPair
          } yield {
            schemaCytoscape(schema)
          }
          for {
            e <- r.attempt
            v <- e.fold(t => responseJson(t.getMessage), Ok(_))
          } yield v
        }
      }

    case req @ GET -> Root / `api` / "schema" / "visualize" :?
        SchemaURLParam(optSchemaURL) +&
        OptSchemaParam(optSchema) +&
        SchemaFormatParam(optSchemaFormatStr) +&
        SchemaEngineParam(optSchemaEngine) +&
        OptActiveSchemaTabParam(optActiveSchemaTab) =>
      val r: EitherT[IO, String, String] = for {
        optSchemaFormat <- optEither2es(
          optSchemaFormatStr,
          SchemaFormat.fromString
        )
        sp = SchemaParam(
          optSchema,
          optSchemaURL,
          None,
          optSchemaFormat,
          optSchemaFormat,
          optSchemaFormat,
          optSchemaFormat,
          optSchemaEngine,
          None,
          None,
          None,
          optActiveSchemaTab
        )
        pair <- EitherT(
          sp.getSchema(None)
            .attempt
            .map(_.leftMap(s => s"Error obtaining schema: ${s.getMessage}"))
        )
        (_, either: Either[String, Schema]) = pair
        svg <- either.fold(
          s => fail_es(s"Error parsing schema: $s"),
          schema => {
            io2es(schema2SVG(schema).map(_._1))
          }
        )
      } yield svg
      for {
        either <- run_es(r)
        v <- either.fold(
          s => responseJson(s"Error obtaining schema $s"),
          svg => {
            Ok(svg).map(
              _.withContentType(`Content-Type`(MediaType.image.`svg+xml`))
            )
          }
        )
      } yield v

    case req @ GET -> Root / `api` / "schema" / "validate" :?
        OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        DataFormatParam(maybeDataFormatStr) +&
        CompoundDataParam(optCompoundData) +&
        OptSchemaParam(optSchema) +&
        SchemaURLParam(optSchemaURL) +&
        SchemaFormatParam(maybeSchemaFormatStr) +&
        SchemaEngineParam(optSchemaEngine) +&
        OptTriggerModeParam(optTriggerMode) +&
        ShapeMapParameterAlt(optShapeMapAlt) +&
        ShapeMapParameter(optShapeMap) +&
        ShapeMapURLParameter(optShapeMapURL) +&
        ShapeMapFileParameter(
          optShapeMapFile
        ) +& // This parameter seems unnecessary...maybe for keeping the state only?
        ShapeMapFormatParam(optShapeMapFormat) +&
        SchemaEmbedded(optSchemaEmbedded) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        //            OptEndpointsParam(optEndpoints) +&
        OptActiveDataTabParam(optActiveDataTab) +&
        OptActiveSchemaTabParam(optActiveSchemaTab) +&
        OptActiveShapeMapTabParam(optActiveShapeMapTab) =>
      val either: Either[String, (Option[DataFormat], Option[SchemaFormat])] =
        for {
          df <- maybeDataFormatStr.map(DataFormat.fromString).sequence
          sf <- maybeSchemaFormatStr.map(SchemaFormat.fromString).sequence
        } yield (df, sf)

      either match {
        case Left(str) => responseJson(str, status = BadRequest)
        case Right(pair) =>
          val (optDataFormat, optSchemaFormat) = pair
          val baseUri                          = req.uri
          logger.info(s"BaseURI: $baseUri")
          logger.info(s"Endpoint: $optEndpoint")
          val dp = DataParam(
            optData,
            optDataURL,
            None,
            optEndpoint,
            optDataFormat,
            optDataFormat,
            optDataFormat,
            None,
            optInference,
            None,
            optActiveDataTab,
            optCompoundData
          )
          val sp = SchemaParam(
            optSchema,
            optSchemaURL,
            None,
            optSchemaFormat,
            optSchemaFormat,
            optSchemaFormat,
            optSchemaFormat,
            optSchemaEngine,
            optSchemaEmbedded,
            None,
            None,
            optActiveSchemaTab
          )
          val collectShapeMap = (optShapeMap, optShapeMapAlt) match {
            case (None, None)     => None
            case (None, Some(sm)) => Some(sm)
            case (Some(sm), None) => Some(sm)
            case (Some(sm1), Some(sm2)) =>
              if(sm1 == sm2) Some(sm1)
              else {
                val msg =
                  s"2 shape-map parameters with different values: $sm1 and $sm2. We use: $sm1"
                logger.error(msg)
                Some(sm1)
              }
          }
          logger.debug(s"collectShapeMap: $collectShapeMap")
          val tp = TriggerModeParam(
            optTriggerMode,
            collectShapeMap,
            optShapeMapFormat,
            optShapeMapURL,
            optShapeMapFormat, // TODO: Maybe a more specific param for URL format?
            optShapeMapFile,
            optShapeMapFormat, // TODO: Maybe a more specific param for File format?
            optActiveShapeMapTab
          )

          val eitherResult: IO[Response[IO]] = for {
            pairData <- io2f(dp.getData(relativeBase))
            (dataStr, resourceRdf) = pairData
            response <- io2f(for {
              resBuilder <- RDFAsJenaModel.empty
              vv <- (resourceRdf, resBuilder).tupled.use {
                case (rdf, builder) =>
                  for {
                    pair <- sp.getSchema(Some(rdf))
                    (schemaStr, eitherSchema) = pair
                    schema <- IO.fromEither(
                      eitherSchema.leftMap(s =>
                        new RuntimeException(s"Error obtaining schema: $s")
                      )
                    )
                    res <- validate(
                      rdf,
                      dp,
                      schema,
                      sp,
                      tp,
                      relativeBase,
                      builder
                    )
                    (result, maybeTrigger, time) = res
                    json <- result2json(res._1)
                  } yield json
              }
            } yield vv)
            v <- Ok(response)
          } yield {
            v
          }
          eitherResult
      }

    case req @ POST -> Root / `api` / "schema" / "validate" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          val r: IO[Json] = for {
            dataPair <- DataParam.mkData(partsMap, relativeBase)
            (resourceRdf, dp) = dataPair
            res <- for {
              emptyRes <- RDFAsJenaModel.empty
              vv <- (resourceRdf, emptyRes).tupled.use { case (rdf, builder) =>
                for {
                  schemaPair <- SchemaParam.mkSchema(partsMap, Some(rdf))
                  (schema, sp) = schemaPair
                  tp     <- TriggerModeParam.mkTriggerModeParam(partsMap)
                  newRdf <- applyInference(rdf, dp.inference)
                  r <- io2f(
                    validate(newRdf, dp, schema, sp, tp, relativeBase, builder)
                  )
                  json <- io2f(result2json(r._1))
                } yield json
              }
            } yield vv
          } yield res

          for {
            e <- r.attempt
            v <- e.fold(t => responseJson(t.getMessage), json => Ok(json))
          } yield v
        }
      }
  }
  private val relativeBase = Defaults.relativeBase

  /** Given an input schema, convert it to another output schema with the parameters specified.
    *
    * @param schema                Input schema
    * @param schemaStr             Input schema contents
    * @param schemaFormat          Input schema format
    * @param schemaEngine          Input schema engine
    * @param optTargetSchemaFormat Output schema desired format
    * @param optTargetSchemaEngine Output schema desired engine
    * @return Optionally, the raw output schema contents
    */
  private[schema] def convertSchema(
      schema: Schema,
      schemaStr: Option[String],
      schemaFormat: SchemaFormat,
      schemaEngine: String,
      optTargetSchemaFormat: Option[SchemaFormat],
      optTargetSchemaEngine: Option[String]
  ): IO[SchemaConversionResult] = {
    val result: IO[SchemaConversionResult] = for {
      pair <- doSchemaConversion(
        schema,
        optTargetSchemaFormat.map(_.name),
        optTargetSchemaEngine
      )
      sourceStr <- schemaStr match {
        case None         => schema.serialize(schemaFormat.name)
        case Some(source) => IO(source)
      }
      (resultStr, resultShapeMap) = pair
    } yield SchemaConversionResult.fromConversion(
      sourceStr,
      schemaFormat.name,
      schemaEngine,
      optTargetSchemaFormat.map(_.name),
      optTargetSchemaEngine,
      resultStr,
      resultShapeMap
    )

    for {
      either <- result.attempt
    } yield either.fold(
      err => SchemaConversionResult.fromMsg(s"error converting schema: $err"),
      identity
    )
  }

  private def doSchemaConversion(
      schema: Schema,
      targetSchemaFormat: Option[String],
      optTargetSchemaEngine: Option[String]
  ): IO[(String, ShapeMap)] = {
    logger.debug(
      s"Schema conversion, name: ${schema.name}, targetSchema: $targetSchemaFormat"
    )
    val default = for {
      str <- schema.convert(targetSchemaFormat, optTargetSchemaEngine, None)
    } yield (str, ShapeMap.empty)
    schema match {
      case shacl: ShaclexSchema =>
        optTargetSchemaEngine.map(_.toUpperCase()) match {
          case Some("SHEX") =>
            logger.debug("Schema conversion: SHACLEX -> SHEX")
            Shacl2ShEx
              .shacl2ShEx(shacl.schema)
              .fold(
                e =>
                  IO.raiseError(
                    new RuntimeException(
                      s"Error converting SHACL -> ShEx: $e"
                    )
                  ),
                pair => {
                  val (schema, shapeMap) = pair
                  logger.debug(s"shapeMap: $shapeMap")
                  for {
                    emptyBuilder <- RDFAsJenaModel.empty
                    str <- emptyBuilder.use(builder =>
                      es.weso.shex.Schema.serialize(
                        schema,
                        targetSchemaFormat.getOrElse("SHEXC"),
                        None,
                        builder
                      )
                    )
                  } yield (str, shapeMap)
                }
              )
          case _ => default
        }
      case _ => default
    }
  }

  private def info(msg: String): EitherT[IO, String, Unit] =
    EitherT.liftF[IO, String, Unit](IO(logger.info(msg)))

  private def applyInference(
      rdf: RDFReasoner,
      inferenceName: Option[String]
  ): IO[RDFReasoner] = inferenceName match {
    case None => IO.pure(rdf)
    case Some(name) =>
      InferenceEngine.fromString(name) match {
        case Left(str) =>
          IO.raiseError(
            new RuntimeException(
              s"Error parsing inference engine: $name: $str"
            )
          )
        case Right(engine) => rdf.applyInference(engine)
      }
  }

  //  private def either2f[A](e: Either[String,A]): F[A] = ???

  // private def

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
