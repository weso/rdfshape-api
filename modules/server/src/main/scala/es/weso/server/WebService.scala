package es.weso.server

import java.util.concurrent.Executors

import es.weso.schema._
import org.http4s.HttpService
import org.http4s.dsl.io._
import cats.effect._
import org.http4s._
import org.http4s.twirl._
import org.http4s.multipart._
import es.weso._
import es.weso.server.QueryParams._
import ApiHelper._
// import cats.effect.IO._
import Defaults._
import ApiHelper._
import cats.data.EitherT
import es.weso.server.helper.{DataFormat, Svg}
import io.circe.Json
import org.http4s.headers.`Content-Type`
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.log4s.getLogger
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{WebjarAsset, Config}
import es.weso.server.utils.IOUtils._
import es.weso.rdf.RDFReader

class WebService[F[_]](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase

  private val logger = getLogger
  val L = implicitly[LiftIO[F]]

  def routes(implicit timer: Timer[F]): HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root => {
      Ok(html.index())
    }

    case req@POST -> Root / "schemaConversions" =>
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        for {
          maybePair <- SchemaParam.mkSchema(partsMap, None).value
          optTargetSchemaFormat <- partsMap.optPartValue("targetSchemaFormat")
          optTargetSchemaEngine <- partsMap.optPartValue("targetSchemaEngine")
          response <- maybePair match {
            case Left(msg) => BadRequest(s"Error obtaining schema: $msg")
            case Right((schema, sp)) => {
              val sv = SchemaValue(sp.schema,
                sp.schemaURL,
                sp.schemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
                sp.schemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
                sp.activeSchemaTab.getOrElse(defaultActiveSchemaTab)
              )
              for {
                eitherCnv <- L.liftIO(schemaConvert(sp.schema,sp.schemaFormat,sp.schemaEngine,optTargetSchemaFormat,optTargetSchemaEngine,ApiHelper.getBase).value)
                ok <- Ok(html.schemaConversions(sv,optTargetSchemaFormat.getOrElse(defaultSchemaFormat),optTargetSchemaEngine.getOrElse(defaultSchemaEngine),eitherCnv))
              } yield ok
            }
          }
        } yield response
      }
      }

    case req@GET -> Root / "schemaConversions" :?
      OptSchemaParam(optSchema) +&
        SchemaURLParam(optSchemaURL) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) +&
        TargetSchemaFormatParam(optTargetSchemaFormat) +&
        TargetSchemaEngineParam(optTargetSchemaEngine) +&
        OptActiveSchemaTabParam(optActiveSchemaTab) => {

      val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat)    
      val schemaEngine = optSchemaEngine.getOrElse(defaultSchemaEngine)
      val targetSchemaFormat = optTargetSchemaFormat.getOrElse(defaultSchemaFormat)
      val targetSchemaEngine = optTargetSchemaEngine.getOrElse(defaultSchemaEngine)
      val sv = SchemaValue(optSchema, optSchemaURL,
        schemaFormat, availableSchemaFormats,
        schemaEngine, availableSchemaEngines,
        optActiveSchemaTab.getOrElse(defaultActiveSchemaTab)
      )
      for {
        cnv <- L.liftIO(schemaConvert(optSchema,optSchemaFormat,optSchemaEngine,optTargetSchemaFormat,optTargetSchemaEngine,ApiHelper.getBase).value)
        ok <- Ok(html.schemaConversions(sv,
          targetSchemaFormat, 
          targetSchemaEngine,
          cnv))
       } yield ok
    }

    case req@POST -> Root / "schemaInfo" =>
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        for {
          maybePair <- SchemaParam.mkSchema(partsMap, None).value
          response <- maybePair match {
           case Left(msg) => BadRequest(s"Error obtaining schema: $msg")
           case Right((schema, sp)) => {
             val sv = SchemaValue(sp.schema,
               sp.schemaURL,
               sp.schemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
               sp.schemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
               sp.activeSchemaTab.getOrElse(defaultActiveSchemaTab)
             )
             val info = schemaInfo(schema)
             Ok(html.schemaInfo(Some(info),sv))
           }
        }
      } yield response
    }
    }

    case req@GET -> Root / "schemaInfo" :?
      OptSchemaParam(optSchema) +&
      SchemaURLParam(optSchemaURL) +&
      SchemaFormatParam(optSchemaFormat) +&
      SchemaEngineParam(optSchemaEngine) +&
      OptActiveSchemaTabParam(optActiveSchemaTab)
    => {
      logger.info(s"GET schemaInfo: schema=$optSchema\nschemaURL: $optSchemaURL")

      val sp = SchemaParam(optSchema, optSchemaURL, None,
        optSchemaFormat, optSchemaFormat, optSchemaFormat, optSchemaEngine,
        None, None, None, optActiveSchemaTab)

      val sv = SchemaValue(optSchema, optSchemaURL,
        optSchemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
        optSchemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
        optActiveSchemaTab.getOrElse(defaultActiveSchemaTab)
      )

      for {
        pair <- L.liftIO(sp.getSchema(None))
        (_,either: Either[String,Schema]) = pair
        v <- either.fold(
          s => BadRequest(s"Error obtaining schema: $s"), 
          schema => { 
           val info = schemaInfo(schema)
           Ok(html.schemaInfo(Some(info),sv))   
          }
        )  
      } yield v
    }

    case req@POST -> Root / "schemaVisualize" =>
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        for {
          maybePair <- SchemaParam.mkSchema(partsMap, None).value
          response <- maybePair match {
            case Left(msg) => BadRequest(s"Error obtaining schema: $msg")
            case Right((schema, sp)) => {
              val sv = SchemaValue(sp.schema,
                sp.schemaURL,
                sp.schemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
                sp.schemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
                sp.activeSchemaTab.getOrElse(defaultActiveSchemaTab)
              )
              val info = schemaVisualize(schema)
              Ok(html.schemaVisualize(Some(info),sv))
            }
          }
        } yield response
      }
      }

    case req@GET -> Root / "schemaVisualize" :?
      OptSchemaParam(optSchema) +&
        SchemaURLParam(optSchemaURL) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) +&
        OptActiveSchemaTabParam(optActiveSchemaTab)
    => {
      logger.info(s"GET schemaVisualize: schema=$optSchema\nschemaURL: $optSchemaURL")

      val sp = SchemaParam(optSchema, optSchemaURL, None,
        optSchemaFormat, optSchemaFormat, optSchemaFormat, optSchemaEngine,
        None, None, None, optActiveSchemaTab)

      val sv = SchemaValue(optSchema, optSchemaURL,
        optSchemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
        optSchemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
        optActiveSchemaTab.getOrElse(defaultActiveSchemaTab)
      )
      for {
        pair <- L.liftIO(sp.getSchema(None))
        (_,either: Either[String,Schema]) = pair
        v <- either.fold(e => BadRequest(s"Error obtaining schema: $e"), schema => {
          val info = schemaVisualize(schema)
          Ok(html.schemaVisualize(Some(info),sv))
        })
      } yield v 
    }

    case req@GET -> Root / "dataOptions" => {
      Ok(html.dataOptions(
        availableDataFormats,
        defaultDataFormat))
    }

    case req@GET -> Root / "schemaOptions" => {
      Ok(html.schemaOptions(
        availableSchemaFormats,
        defaultSchemaFormat,
        availableSchemaEngines,
        defaultSchemaEngine,
        availableTriggerModes,
        defaultTriggerMode))
    }

    case req@GET -> Root / "load" :?
      ExamplesParam(examples) +& ManifestURLParam(manifestURL) => {
      (examples,manifestURL) match {
        case (None,None) => BadRequest(s"Missing parameter 'examples' or 'manifestURL'")
        case (Some(ex),None) => Ok(html.load(ex))
        case (None,Some(ex)) => Ok(html.load(ex))
        case (Some(ex1),Some(ex2)) =>
          if (ex1 == ex2) Ok(html.load(ex1))
          else BadRequest(s"Parameter 'examples' and 'manifestURL' are different, select one of them")
      }
    }

    case req@POST -> Root / "validate" =>
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        logger.info(s"POST validate partsMap. $partsMap")
        val r: ESF[Response[F],F] = for {
          dataPair <- DataParam.mkData(partsMap,relativeBase)
          (rdf, dp) = dataPair
          schemaPair <- SchemaParam.mkSchema(partsMap, Some(rdf))
          (schema, sp) = schemaPair
          tp <- TriggerModeParam.mkTriggerModeParam(partsMap)
          pair <- io2esf(validate(rdf,dp, schema, sp, tp, relativeBase))
          (result, maybeTrigger, time) = pair
          ok <- validateResponse(result, time, dp, sp, tp)
        } yield {
          ok
        }
        r.value.flatMap(_.fold(e => BadRequest(e), F.pure(_)))
      }
  }

  case req@GET -> Root / "validate" :?
      OptExamplesParam(optExamples) +&
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
      ShapeMapParameter(optShapeMapFirst) +&
      ShapeMapParameterAlt(optShapeMapAlt) +&
      ShapeMapURLParameter(optShapeMapURL) +&
      ShapeMapFileParameter(optShapeMapFile) +&
      ShapeMapFormatParam(optShapeMapFormat) +&
      SchemaEmbedded(optSchemaEmbedded) +&
      InferenceParam(optInference) +&
      OptEndpointParam(optEndpoint) +&
      OptActiveDataTabParam(optActiveDataTab) +&
      OptActiveSchemaTabParam(optActiveSchemaTab) +&
      OptActiveShapeMapTabParam(optActiveShapeMapTab) => 
      Ok(s"Web Service has been deprecated in favor of ReactClient")
      /* {
      if (optExamples.isDefined) {
        Ok(html.load(optExamples.get))
      } else {
        val either: Either[String, Option[DataFormat]] =  for {
          df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
        } yield df

        either match {
          case Left(str) => BadRequest(str)
          case Right(optDataFormat) => {
            val baseUri = req.uri
            logger.info(s"BaseURI: $baseUri")
            logger.info(s"Endpoint: $optEndpoint")
            val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, optDataFormat, None, optInference, None, optActiveDataTab)
            val sp = SchemaParam(optSchema, optSchemaURL, None, optSchemaFormat, optSchemaFormat, optSchemaFormat, optSchemaEngine, optSchemaEmbedded, None, None, optActiveSchemaTab)
            val optShapeMap = (optShapeMapFirst, optShapeMapAlt) match {
              case (Some(s1), Some(s2)) if s1 == s2 => {
                Some(s1)
              }
              case (Some(s1), Some(s2)) => {
                logger.info(s"Two values for shapeMap param, using $s1 and omitting $s2")
                Some(s1)
              }
              case (Some(s1), None) => Some(s1)
              case (None, Some(s2)) => Some(s2)
              case (None, None) => None
            }
            val tp = TriggerModeParam(
              optTriggerMode,
              optShapeMap,
              optShapeMapFormat,
              optShapeMapURL,
              optShapeMapFormat, // TODO: Maybe a more specific param for URL format?
              optShapeMapFile,
              optShapeMapFormat, // TODO: Maybe a more specific param for File format?
              optActiveShapeMapTab
            )
            logger.info(s"OptSchema: $optSchema")
            logger.info(s"OptSchemaFormat: $optSchemaFormat")

            val (dataStr, eitherRDF) = dp.getData(relativeBase)

            val eitherResult: EitherT[F,String,F[Response[F]]] = for {
              rdf <- EitherT.fromEither[F](eitherRDF)
              pair <- EitherT.liftF[F,String,(Option[String], Either[String,Schema])](L.liftIO(sp.getSchema(Some(rdf))))
              (_,either: Either[String,Schema]) = pair
              schema <- EitherT.fromEither[F](either)
              // (schemaStr, eitherSchema) = sp.getSchema(Some(rdf))
              // schema <- eitherSchema
            } yield {
              val (result, maybeTrigger, time) = validate(rdf, dp, schema, sp, tp,relativeBase)
              validateResponse(result, time, dp, sp, tp)
            }

            eitherResult.foldF(e => BadRequest(s"Error: $e"),identity)
          }
        }
      }
    }
*/
    case req@GET -> Root / "query" :?
      OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        DataFormatParam(maybeDataFormat) +&
        OptQueryParam(optQuery) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        OptActiveDataTabParam(optActiveDataTab) +&
        OptActiveQueryTabParam(optActiveQueryTab)
        => Ok(s"Web Service has been deprecated in favor of Javascript Client")
/*        {
      val either: Either[String, Option[DataFormat]] =  for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => BadRequest(str)
        case Right(optDataFormat) => {
          val result = query(optData.getOrElse(""), optDataFormat, optQuery, optInference)
          val dv = DataValue(optData,
            optDataURL,
            optDataFormat.getOrElse(defaultDataFormat),
            availableDataFormats,
            optInference.getOrElse(defaultInference),
            availableInferenceEngines,
            optEndpoint,
            optActiveDataTab.getOrElse(defaultActiveDataTab)
          )
          Ok(html.query(
            result,
            dv,
            optQuery,
            optActiveQueryTab.getOrElse(defaultActiveQueryTab)
          ))
        }
      }
    } */

    case req@POST -> Root / "query" => {
      req.decode[Multipart[F]] { m => /* {
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap,relativeBase).value
          response <- maybeData match {
            case Left(msg) => BadRequest(s"Error obtaining data: $msg")
            case Right((rdf, dp)) => for {
              maybePair <- SparqlQueryParam.mkQuery(partsMap)
              response <- maybePair match {
                case Left(msg) => BadRequest(s"Error obtaining query: $msg")
                case Right((queryStr, qp)) => {
                  val optQueryStr = qp.query.map(_.str)
                  val result = rdf.queryAsJson(optQueryStr.getOrElse(""))
                  logger.info(s"Result: ${result}")
                  val dv = DataValue(
                    dp.data, dp.dataURL,
                    dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
                    dp.inference.getOrElse(defaultInference), availableInferenceEngines,
                    dp.maybeEndpoint,
                    dp.activeDataTab.getOrElse(defaultActiveDataTab)
                  )
                  Ok(html.query(
                    result,
                    dv,
                    optQueryStr,
                    qp.activeQueryTab.getOrElse(defaultActiveQueryTab)
                  ))
                }
              }
            } yield response
          }
        } yield response
       } */
       Ok(s"Web service has been deprecated in favor of Javascript client")
      } 
    }

    case req@GET -> Root / "shapeInfer" :?
      OptDataParam(optData) +&
      OptDataURLParam(optDataURL) +&
      DataFormatParam(maybeDataFormat) +&
      OptEndpointParam(optEndpoint) +&
      InferenceParam(optInference) +&
      SchemaEngineParam(optSchemaEngineParam) +&
      SchemaFormatParam(optSchemaFormatParam) +&
      OptActiveDataTabParam(optActiveDataTab) +&
      OptNodeSelectorParam(optNodeSelectorParam)
    => {
      val either: Either[String, Option[DataFormat]] =  for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => BadRequest(str)
        case Right(optDataFormat) => /* {
          val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, optDataFormat, None, optInference, None, optActiveDataTab)
          val (dataStr, eitherRDF) = dp.getData(relativeBase)
          val dv = DataValue(optData,
            optDataURL,
            optDataFormat.getOrElse(defaultDataFormat),
            availableDataFormats,
            optInference.getOrElse(defaultInference),
            availableInferenceEngines,
            optEndpoint,
            optActiveDataTab.getOrElse(defaultActiveDataTab)
          )
          val eitherResult: Either[String,Json] = for {
            rdf <- eitherRDF
            jsonResult <- ApiHelper.shapeInfer(rdf, optNodeSelectorParam, optInference, optSchemaEngineParam, optSchemaFormatParam, None, relativeBase, true)
          } yield {
            jsonResult
          }

          eitherResult.fold(e => BadRequest(s"Error: $e"),r => {
            Ok(html.shapeInfer(
              Some(r),
              dv,
              availableSchemaEngines,
              optSchemaEngineParam.getOrElse(defaultSchemaEngine),
              availableSchemaFormats,
              optSchemaFormatParam.getOrElse(defaultSchemaFormat),
              optNodeSelectorParam.getOrElse(""),
              "Shape"
            ))
          }
          ) */
          Ok(s"Web service has been deprecated")
      }
    }

    case req@POST -> Root / "shapeInfer" => 
       Ok(s"Web service has been deprecated")
    /* {
      req.decode[Multipart[F]] { m => {
        val partsMap = PartsMap(m.parts)
        val r = for {
          dataPair <- DataParam.mkData(partsMap,relativeBase)
          (rdf, dp) = dataPair
          nodeSelector <- EitherT(partsMap.eitherPartValue("nodeSelector"))
          schemaEngine <- EitherT(partsMap.eitherPartValue("schemaEngine"))
          schemaFormat <- EitherT(partsMap.eitherPartValue("schemaFormatTextArea"))
          jsonResult <- EitherT.fromEither[F](shapeInfer(rdf, Some(nodeSelector), dp.inference, Some(schemaEngine), Some(schemaFormat), None, relativeBase, true))
        } yield {
          val dv = DataValue(dp.data,dp.dataURL,dp.dataFormat.getOrElse(defaultDataFormat),availableDataFormats,
            dp.inference.getOrElse(defaultInference),
            availableInferenceEngines,
            dp.maybeEndpoint,
            dp.activeDataTab.getOrElse(defaultActiveDataTab)
          )
          Ok(html.shapeInfer(
            Some(jsonResult),
            dv,
            availableSchemaEngines,
            schemaEngine,
            availableSchemaFormats,
            schemaFormat,
            nodeSelector,
            "Shape"
          ))
        }
        // r.value.unsafeRunSync().fold(e => BadRequest(s"Error: $e"),identity)
        for {
          e <- r.value
          v <- e.fold(BadRequest(_), identity)
        } yield v
      } */
  }

  private def err[A](str: String): Either[String, A] = {
    Left(str)
  }

  private[server] def validateResponse(result: Result,
                                       time: Long,
                                       dp: DataParam,
                                       sp: SchemaParam,
                                       tp: TriggerModeParam): ESF[Response[F],F] = {
    val dv = DataValue(
      dp.data, dp.dataURL,
      dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
      dp.inference.getOrElse(defaultInference), availableInferenceEngines,
      dp.maybeEndpoint,
      dp.activeDataTab.getOrElse(defaultActiveDataTab)
    )
    val sv = SchemaValue(sp.schema,
      sp.schemaURL,
      sp.schemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
      sp.schemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
      sp.activeSchemaTab.getOrElse(defaultActiveSchemaTab)
    )
    val smv = ShapeMapValue(
      tp.shapeMap, tp.shapeMapURL,
      tp.shapeMapFormat.getOrElse(defaultShapeMapFormat),
      availableShapeMapFormats,
      tp.activeShapeMapTab.getOrElse(defaultActiveShapeMapTab)
    )

    val r: ESF[Response[F],F] = for {
      validationReport <- either2ef[RDFReader,F](result.validationReport)
      strRdf <- io2esf(validationReport.serialize("TURTLE"))
      ok <- f2es(Ok(html.validate(Some(result),time,Some(Right(strRdf)),dv, sv, availableTriggerModes,tp.triggerMode.getOrElse(defaultTriggerMode),
      smv,getSchemaEmbedded(sp))))
    } yield ok
    r
  }

  private def allNone(maybes: Option[String]*) = {
    maybes.forall(_.isEmpty)
  }

}

object WebService {
  def apply[F[_]: Effect: ContextShift](blocker: Blocker): WebService[F] =
    new WebService[F](blocker)
}
