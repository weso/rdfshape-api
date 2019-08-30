package es.weso.server

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.APIDefinitions._
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

class SchemaService[F[_]:ConcurrentEffect: Timer](blocker: Blocker,
                                               client: Client[F])(implicit cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase
  private val logger = getLogger

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

    case req@GET -> Root / `api` / "schema" / "info" :?
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

    case req@GET -> Root / `api` / "schema" / "convert" :?
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

    case req@GET -> Root / `api` / "schema" / "validate" :?
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
        ShapeMapFileParameter(optShapeMapFile) +& // This parameter seems unnecessary...maybe for keeping the state only?
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
          val collectShapeMap = (optShapeMap, optShapeMapAlt) match {
            case (None, None) => None
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

    case req@POST -> Root / `api` / "schema" / "validate" =>
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        logger.info(s"POST validate partsMap. $partsMap")
        val r: EitherT[F,String,Result] = for {
          dataPair <- DataParam.mkData(partsMap, relativeBase)
          (rdf, dp) = dataPair
          schemaPair <- SchemaParam.mkSchema(partsMap, Some(rdf))
          (schema, sp) = schemaPair
          tp <- TriggerModeParam.mkTriggerModeParam(partsMap)
        } yield {
          // val schemaEmbedded = getSchemaEmbedded(sp)
          println(s"Trigger mode: $tp")
          val (result, maybeTriggerMode, time) = validate(rdf, dp, schema, sp, tp, relativeBase)
          result
        }
        for {
          e <- r.value
          v <- e.fold(errJson(_), r => Ok(r.toJson))
        } yield v
      }
      }
  }

  // TODO: Move this method to a more generic place...
  private def errJson(msg: String): F[Response[F]] =
    Ok(Json.fromFields(List(("error",Json.fromString(msg)))))

}

object SchemaService {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](blocker: Blocker, client: Client[F]): SchemaService[F] =
    new SchemaService[F](blocker, client)
}

