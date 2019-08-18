package es.weso.server

import cats.effect._
import cats.implicits._
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.ApiHelper._
import es.weso.server.Defaults.defaultDataFormat
import es.weso.server.QueryParams._
import es.weso.server.helper.DataFormat
import es.weso.server.results._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.log4s.getLogger

class APIService[F[_]:ConcurrentEffect: Timer](blocker: Blocker,
                                               client: Client[F])(implicit cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase
  private val logger = getLogger
  val api = "api"

  val wikidataEntityUrl = "http://www.wikidata.org/entity/Q"

  //  val version = "1.0"

  private val swagger =
    resourceService[F](ResourceService.Config("/swagger", blocker))

  val routes = HttpRoutes.of[F] {

    case GET -> Root / `api` / "schema" / "engines" => {
      val engines = Schemas.availableSchemaNames
      val json = Json.fromValues(engines.map(str => Json.fromString(str)))
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "engines" / "default" => {
      val schemaEngine = Schemas.defaultSchemaName
      val json = Json.fromString(schemaEngine)
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "formats" => {
      val formats = Schemas.availableFormats
      val json = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "triggerModes" => {
      val triggerModes = ValidationTrigger.triggerValues.map(_._1)
      val json = Json.fromValues(triggerModes.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "data" / "formats" => {
      val formats = DataFormats.formatNames
      val json = Json.fromValues(formats.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "data" / "visualize" / "formats" => {
      val formats = DataConverter.availableGraphFormatNames ++
        List(
          "DOT", // DOT is not a visual format but can be used to debug
          "JSON"  // JSON is the format that can be used by Cytoscape
        )
      val json = Json.fromValues(formats.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "data" / "formats" / "default" => {
      val dataFormat = DataFormats.defaultFormatName
      Ok(Json.fromString(dataFormat))
    }

    case req @ GET -> Root / `api` / "dataUrl" / "info" :?
      OptDataURLParam(optDataUrl) +&
      DataFormatParam(optDataFormat) => {
      val dataFormat = dataFormatOrDefault(optDataFormat)
      optDataUrl match {
          case None => errJson(s"Must provide a dataUrl")
          case Some(dataUrl) => client.expect[String](dataUrl).flatMap(data => {
            val result = dataInfoFromString(data,dataFormat)
            Ok(result).map(_.withContentType(`Content-Type`(MediaType.application.json)))
          })
     }
    }

    case req @ POST -> Root / `api` / "data" / "info" => {
      println(s"POST /api/data/info, Request: $req")
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(err) => errJson(
              s"""|Error obtaining RDF data
                  |$err""".stripMargin
            )
            case Right((rdf, dp)) => {
              val dataFormat = dataFormatOrDefault(dp.dataFormat.map(_.name))
              dp.data match {
                case Some(data) => Ok(dataInfoFromString(data, dataFormat))
                case None => Ok(DataInfoResult.fromMsg("No data").toJson)
              }
            }
          }
        } yield response
      }
    }

    case req @ GET -> Root / `api` / "data" / "info" :?
      DataParameter(data) +&
        DataFormatParam(optDataFormat) => {
      val dataFormat = dataFormatOrDefault(optDataFormat)
      val result = dataInfoFromString(data, dataFormat)
      Ok(result).map(_.withContentType(`Content-Type`(MediaType.application.json)))
    }

    case req @ GET -> Root / `api` / "schema" / "info" :?
      OptSchemaParam(optSchema) +&
      SchemaFormatParam(optSchemaFormat) +&
      SchemaEngineParam(optSchemaEngine) => {
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val schemaStr = optSchema match {
        case None => ""
        case Some(schema) => schema
      }
      Schemas.fromString(schemaStr, schemaFormat, schemaEngine, None) match {
        case Left(e) => errJson(s"Error reading schema: $e\nString: $schemaStr")
        case Right(schema) => {
          val shapes: List[String] = schema.shapes
          val jsonShapes = Json.fromValues(shapes.map(Json.fromString(_)))
          val pm: Json = prefixMap2Json(schema.pm)
          val result = SchemaInfoResult(schemaStr, schemaFormat, schemaEngine, jsonShapes, pm).asJson
          Ok(result).map(_.withContentType(`Content-Type`(MediaType.application.json)))
        }
      }
    }

    case req @ POST -> Root / `api` / "data" / "convert" => {
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(msg) => errJson(s"Error obtaining data: $msg")
            case Right((rdf, dp)) => {
              val targetFormat = dp.targetDataFormat.getOrElse(defaultDataFormat).name
              val dataFormat = dp.dataFormat.getOrElse(defaultDataFormat)
              println(s"### POST DataFormat = ${dataFormat}")
              val either = DataConverter.rdfConvert(rdf, dp.data, dataFormat, targetFormat)
              either.fold(e => errJson(e), r => Ok(r.toJson))
            }
          }
        } yield response
      }
    }

    case req @ GET -> Root / `api` / "data" / "convert" :?
      DataParameter(data) +&
      DataFormatParam(optDataFormat) +&
      TargetDataFormatParam(optResultDataFormat) => {
      val either = for {
        dataFormat <- DataFormat.fromString(optDataFormat.getOrElse(defaultDataFormat.name))
        s <- DataConverter.dataConvert(data,dataFormat,optResultDataFormat.getOrElse(defaultDataFormat.name))
      } yield s
      either.fold(e => errJson(s"Error: $e"),r => Ok(r.toJson))
    }

    case req @ POST -> Root / `api` / "data" / "query" => {
      println(s"POST /api/data/query, Request: $req")
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(err) => errJson(
              s"""|Error obtaining RDF data
                  |$err""".stripMargin
            )
            case Right((rdf, dp)) => {
              val dataFormat = dataFormatOrDefault(dp.dataFormat.map(_.name))
              dp.data match {
                case Some(data) => errJson("Not implemented yet Data Query")
                case None => errJson("No data provided")
              }
            }
          }
        } yield response
      }
    }

    case req @ POST -> Root / `api` / "data" / "extract" => {
      println(s"POST /api/data/extract, Request: $req")
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(err) => errJson(
              s"""|Error obtaining RDF data
                  |$err""".stripMargin
            )
            case Right((rdf, dp)) => {
              val dataFormat = dataFormatOrDefault(dp.dataFormat.map(_.name))
              dp.data match {
                case Some(data) => errJson("Not implemented yet extract Schema as API")
                case None => errJson("No data provided")
              }
            }
          }
        } yield response
      }
    }

    case req @ GET -> Root / `api` / "schema" / "convert" :?
      OptSchemaParam(optSchema) +&
      SchemaFormatParam(optSchemaFormat) +&
      SchemaEngineParam(optSchemaEngine) +&
      TargetSchemaFormatParam(optResultSchemaFormat) +&
      TargetSchemaEngineParam(optResultSchemaEngine) => {
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val resultSchemaFormat = optResultSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val resultSchemaEngine = optResultSchemaEngine.getOrElse(Schemas.defaultSchemaName)

      val schemaStr = optSchema match {
        case None => ""
        case Some(schema) => schema
      }
      Schemas.fromString(schemaStr, schemaFormat, schemaEngine, None) match {
        case Left(e) => errJson(s"Error reading schema: $e\nString: $schemaStr")
        case Right(schema) => {
          if (schemaEngine.toUpperCase == resultSchemaEngine.toUpperCase) {
            schema.serialize(resultSchemaFormat) match {
              case Right(resultStr) => {
                val result = SchemaConversionResult(schemaStr, schemaFormat, schemaEngine,
                  resultSchemaFormat, resultSchemaEngine, resultStr)
                val default = Ok(result.asJson)
                  .map(_.withContentType(`Content-Type`(MediaType.application.json)))
                req.headers.get(`Accept`) match {
                  case Some(ah) => {
                    logger.info(s"Accept header: $ah")
                    val hasHTML: Boolean = ah.values.exists(mr => mr.mediaRange.satisfiedBy(MediaType.text.html))
                    if (hasHTML) {
                      Ok(result.toHTML).map(_.withContentType(`Content-Type`(MediaType.text.html)))
                    } else default
                  }
                  case None => default
                }
              }
              case Left(e) =>
                errJson(s"Error serializing $schemaStr with $resultSchemaFormat/$resultSchemaEngine: $e")
            }
          } else {
            errJson(s"Conversion between different schema engines not implemented yet: $schemaEngine/$resultSchemaEngine")
          }
        }
      }
    }

    case req @ (GET | POST) -> Root / `api` / "validate" :?
      OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        DataFormatParam(maybeDataFormat) +&
        OptSchemaParam(optSchema) +&
        SchemaURLParam(optSchemaURL) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) +&
        OptTriggerModeParam(optTriggerMode) +&
        NodeParam(optNode) +&
        ShapeParam(optShape) +&
        ShapeMapParameterAlt(optShapeMapAlt) +&
        ShapeMapParameter(optShapeMap) +&
        ShapeMapURLParameter(optShapeMapURL) +&
        ShapeMapFileParameter(optShapeMapFile) +&  // This parameter seems unnecessary...maybe for keeping the state only?
        ShapeMapFormatParam(optShapeMapFormat) +&
        SchemaEmbedded(optSchemaEmbedded) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        OptActiveDataTabParam(optActiveDataTab) +&
        OptActiveSchemaTabParam(optActiveSchemaTab) +&
        OptActiveShapeMapTabParam(optActiveShapeMapTab) => {
      val either: Either[String, Option[DataFormat]] = for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => errJson(str)
        case Right(optDataFormat) => {
          val baseUri = req.uri
          logger.info(s"BaseURI: $baseUri")
          logger.info(s"Endpoint: $optEndpoint")
          val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, optDataFormat, None, optInference, None, optActiveDataTab)
          val sp = SchemaParam(optSchema, optSchemaURL, None, optSchemaFormat, optSchemaFormat, optSchemaFormat, optSchemaEngine, optSchemaEmbedded, None, None, optActiveSchemaTab)
          val collectShapeMap = (optShapeMap,optShapeMapAlt) match {
            case (None,None) => None
            case (None, Some(sm)) => Some(sm)
            case (Some(sm), None) => Some(sm)
            case (Some(sm1), Some(sm2)) => if (sm1 == sm2) Some(sm1)
              else {
                val msg = (s"2 shape-map paramters with different values: $sm1 and $sm2. We use: $sm1")
                logger.error(msg)
                println(msg)
                Some(sm1)
              }
           }
          println(s"#### optShapeMap: ${collectShapeMap}")
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
          val (dataStr, eitherRDF) = dp.getData(relativeBase)

          val eitherResult: Either[String, F[Response[F]]] = for {
            rdf <- eitherRDF
            (schemaStr, eitherSchema) = sp.getSchema(Some(rdf))
            schema <- eitherSchema
          } yield {
            println(s"RDF: $rdf")
            println(s"Schema: $schema")
            val (result, maybeTrigger, time) = validate(rdf, dp, schema, sp, tp, relativeBase)
            println(s"maybeTrigger: $maybeTrigger")
            println(s"result: $result")
            Ok(result.toJson)
          }
          eitherResult.fold(e => errJson(s"Error: $e"), identity)
        }
      }
    }

    case req @ GET -> Root / `api` / "endpoint" / "outgoing" :?
      OptEndpointParam(optEndpoint) +&
      OptNodeParam(optNode)
      => optEndpoint match {
        case None => mkErr("No endpoint provided")
        case Some(endpoint) => optNode match {
            case None => mkErr("No node provided")
            case Some(node) => Ok(Streams.getOutgoing(endpoint, node))
        }
      }

    case req @ GET -> Root / `api` / "wikidata" / "entity" :?
      OptEntityParam(optEntity) => {
       optEntity match {
        case None => mkErr("No entity provided")
        case Some(entity) => {
          getWikidataUri(entity).fold(
            e => mkErr(e),
            uri => Ok(Streams.getRaw(uri))
          )
        }
      }
    }

    // Contents on /swagger are directly mapped to /swagger
    case r @ GET -> _ if r.pathInfo.startsWith("/swagger/") => swagger(r).getOrElseF(NotFound())

  }

  private def mkErr(msg: String): F[Response[F]] =
    Ok(Json.fromFields(List(("error", Json.fromString(msg)))))

  private def getWikidataUri(entity: String): Either[String, Uri] = {
    println(s"getWikidataUri: $entity")
    val q = """Q(\d*)""".r
    entity match {
      case q(n) => Uri.fromString(wikidataEntityUrl + n).leftMap(f => s"Error creating URI for entity ${n}: ${f}")
      case _ => Uri.fromString(entity).leftMap(f => s"Error creating URI from $entity: $f")
    }
  }

  private def errJson(msg: String): F[Response[F]] =
    Ok(Json.fromFields(List(("error",Json.fromString(msg)))))
}


