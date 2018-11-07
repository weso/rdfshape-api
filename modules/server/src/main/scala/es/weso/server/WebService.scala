package es.weso.server

import es.weso.schema._
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.server.staticcontent.ResourceService.Config
import cats.effect._
import org.http4s._
import org.http4s.twirl._
import Http4sUtils._
import org.http4s.multipart._
import es.weso._
import es.weso.server.QueryParams._
import ApiHelper.{query, _}
import cats.effect.IO._
import Defaults._
import ApiHelper._
import cats.data.EitherT
import es.weso.server.helper.{DataFormat, Svg}
import io.circe.Json
import org.http4s.MediaType._
import org.http4s.headers.`Content-Type`
// import cats._
// import cats.data._
import cats.implicits._
import org.log4s.getLogger

object WebService {

  // Get the static content
  private val static: HttpService[IO] =
    staticResource[IO](Config("/static", "/static"))

  private val views: HttpService[IO] =
    staticResource(Config("/staticviews", "/"))

  private val logger = getLogger

  val webService: HttpService[IO] = HttpService[IO] {

    case req@GET -> Root => {
      Ok(html.index())
    }

    case req@GET -> Root / "dataConversions" :?
      OptDataParam(optData) +&
      OptDataURLParam(optDataURL) +&
      DataFormatParam(maybeDataFormat) +&
      InferenceParam(optInference) +&
      OptEndpointParam(optEndpoint) +&
      OptActiveDataTabParam(optActiveDataTab) +&
      TargetDataFormatParam(maybeTargetDataFormat) => {
      val either: Either[String, (Option[DataFormat],Option[DataFormat])] =  for {
         df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
         tdf <- maybeTargetDataFormat.map(DataFormat.fromString(_)).sequence
       } yield (df, tdf)

      either match {
        case Left(str) => BadRequest(str)
        case Right(values) => {
          val (optDataFormat,optTargetDataFormat) = values
          val dp =
            DataParam(optData, optDataURL, None, optEndpoint,
              optDataFormat, optDataFormat,
              None,  //no dataFormatFile
              optInference,
              optTargetDataFormat,
              optActiveDataTab)

          val dv = DataValue(optData,
            optDataURL,
            optDataFormat.getOrElse(defaultDataFormat),
            availableDataFormats,
            optInference.getOrElse(defaultInference),
            availableInferenceEngines,
            optEndpoint,
            optActiveDataTab.getOrElse(defaultActiveDataTab)
          )
          val (maybeStr, eitherRDF) = dp.getData
          println(s"GET dataConversions: $maybeStr\nEitherRDF:${eitherRDF}\ndp: ${dp}\ndv: $dv")
          val result = if (allNone(optData,optDataURL,optEndpoint))
            Right(None)
          else for {
            rdf <- eitherRDF
            str <- rdf.serialize(optTargetDataFormat.getOrElse(defaultDataFormat).name)
          } yield Some(str)
          Ok(html.dataConversions(dv,optTargetDataFormat.getOrElse(defaultDataFormat),result))
        }
      }
    }

    case req@POST -> Root / "dataConversions" => {
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap).value
          response <- maybeData match {
            case Left(msg) => BadRequest(s"Error obtaining data: $msg")
            case Right((rdf,dp)) => {
              val targetFormat = dp.targetDataFormat.getOrElse(defaultDataFormat)
              val dv = DataValue(
                dp.data, dp.dataURL,
                dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
                dp.inference.getOrElse(defaultInference), availableInferenceEngines,
                dp.endpoint,
                dp.activeDataTab.getOrElse(defaultActiveDataTab)
              )
              val (maybeStr, eitherRDF) = dp.getData
              val result = for {
                rdf <- eitherRDF
                str <- rdf.serialize(targetFormat.name)
              } yield Some(str)
              Ok(html.dataConversions(dv,
                  dp.targetDataFormat.getOrElse(defaultDataFormat),
                  result))
            }
          }
        } yield response
      }
    }

    case req@GET -> Root / "dataInfo" :?
      OptDataParam(optData) +&
      OptDataURLParam(optDataURL) +&
      DataFormatParam(maybeDataFormat) +&
      InferenceParam(optInference) +&
      OptEndpointParam(optEndpoint) +&
      OptActiveDataTabParam(optActiveDataTab) => {
      val either: Either[String, Option[DataFormat]] =  for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => BadRequest(str)
        case Right(optDataFormat) => {
          val dp =
            DataParam(optData, optDataURL, None, optEndpoint,
              optDataFormat, optDataFormat,
              None,  //no dataFormatFile
              optInference,
              None, optActiveDataTab)
          val (maybeStr, eitherRDF) = dp.getData
          eitherRDF.fold(
            str => BadRequest(str),
            rdf => {
              val dv = DataValue(optData,
                optDataURL,
                optDataFormat.getOrElse(defaultDataFormat),
                availableDataFormats,
                optInference.getOrElse(defaultInference),
                availableInferenceEngines,
                optEndpoint,
                optActiveDataTab.getOrElse(defaultActiveDataTab)
              )
              Ok(html.dataInfo(dataInfo(rdf),dv))
            })
        }
      }
    }

    case req@POST -> Root / "dataInfo" => {
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap).value
          response <- maybeData match {
            case Left(str) => BadRequest (s"Error obtaining data: $str")
            case Right((rdf,dp)) => {
              val dv = DataValue(
                dp.data, dp.dataURL,
                dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
                dp.inference.getOrElse(defaultInference), availableInferenceEngines,
                dp.endpoint,
                dp.activeDataTab.getOrElse(defaultActiveDataTab)
              )
              Ok(html.dataInfo(dataInfo(rdf),dv))
            }
          }
        } yield response
      }
    }

    case req@GET -> Root / "dataVisualization" :?
      OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        DataFormatParam(maybeDataFormat) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        OptActiveDataTabParam(optActiveDataTab) +&
        TargetDataFormatParam(maybeTargetDataFormat) => {

      val either: Either[String, (Option[DataFormat],Option[DataFormat])] =  for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
        tdf <- maybeTargetDataFormat.map(DataFormat.fromString(_)).sequence
      } yield (df, tdf)

      either match {
        case Left(str) => BadRequest(str)
        case Right(values) => {
          val (optDataFormat,optTargetDataFormat) = values
          val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, None,
            optInference, None, optActiveDataTab
          )
          val (maybeStr, eitherRDF) = dp.getData
          eitherRDF.fold(
            str => BadRequest(str),
            rdf => {
              val (optDataFormat, optTargetDataFormat) = values
              val dv = DataValue(optData,
                optDataURL,
                optDataFormat.getOrElse(defaultDataFormat),
                availableDataFormats,
                optInference.getOrElse(defaultInference),
                availableInferenceEngines,
                optEndpoint,
                optActiveDataTab.getOrElse(defaultActiveDataTab)
              )
              val targetDataFormat = optTargetDataFormat.getOrElse(Svg)
              Ok(html.dataVisualization(dv,targetDataFormat))
            })
        }
      }

    }


    case req@POST -> Root / "dataVisualization" => {
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap).value
          response <- maybeData match {
            case Left(str) => BadRequest (s"Error obtaining data: $str")
            case Right((rdf,dp)) => {
/*              val dv = DataValue(
                dp.data, dp.dataURL,
                dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
                dp.inference.getOrElse(defaultInference), availableInferenceEngines,
                dp.endpoint,
                dp.activeDataTab.getOrElse(defaultActiveDataTab)
              ) */
              val targetFormat = dp.targetDataFormat.getOrElse(Svg)
              DataConverter.rdfConvert(rdf,targetFormat.name).
                fold(e => BadRequest(s"Error in conversion to $targetFormat: $e\nRDF:\n${rdf.serialize("TURTLE")}"),
                  result => Ok(result).map(_.withContentType(`Content-Type`(targetFormat.mimeType)))
                )
            }
          }
        } yield response
      }
    }

    case req@POST -> Root / "schemaConversions" =>
      req.decode[Multipart[IO]] { m => {
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
              Ok(html.schemaConversions(
                sv,
                optTargetSchemaFormat.getOrElse(defaultSchemaFormat),
                optTargetSchemaEngine.getOrElse(defaultSchemaEngine),
                schemaConvert(sp.schema,sp.schemaFormat,sp.schemaEngine,
                  optTargetSchemaFormat,optTargetSchemaEngine,
                  ApiHelper.getBase))
              )
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

      val sv = SchemaValue(optSchema, optSchemaURL,
        optSchemaFormat.getOrElse(defaultSchemaFormat), availableSchemaFormats,
        optSchemaEngine.getOrElse(defaultSchemaEngine), availableSchemaEngines,
        optActiveSchemaTab.getOrElse(defaultActiveSchemaTab)
      )
      Ok(html.schemaConversions(
        sv,
        optTargetSchemaFormat.getOrElse(defaultSchemaFormat),
        optTargetSchemaEngine.getOrElse(defaultSchemaEngine),
        schemaConvert(optSchema,optSchemaFormat,optSchemaEngine,
          optTargetSchemaFormat,optTargetSchemaEngine,
          ApiHelper.getBase))
      )
    }

    case req@POST -> Root / "schemaInfo" =>
      req.decode[Multipart[IO]] { m => {
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
      val (maybeStr, eitherSchema) = sp.getSchema(None)

      eitherSchema.fold(
        e => BadRequest(s"Error obtaining schema: $e"),
        schema => {
         val info = schemaInfo(schema)
         Ok(html.schemaInfo(Some(info),sv))
      })
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

    case req@GET -> Root / "about" => {
      Ok(html.about())
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
      req.decode[Multipart[IO]] { m => {
        val partsMap = PartsMap(m.parts)
        logger.info(s"POST validate partsMap. $partsMap")
        val r = for {
          dataPair <- DataParam.mkData(partsMap)
          (rdf, dp) = dataPair
          schemaPair <- SchemaParam.mkSchema(partsMap, Some(rdf))
          (schema, sp) = schemaPair
          tp <- TriggerModeParam.mkTriggerModeParam(partsMap)
        } yield {
          // val schemaEmbedded = getSchemaEmbedded(sp)
          val (result, maybeTriggerMode, time) = validate(rdf,dp, schema, sp, tp)
          validateResponse(result, time, dp, sp, tp)
        }
      r.value.unsafeRunSync.fold(e => BadRequest(e), identity)
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
      OptActiveShapeMapTabParam(optActiveShapeMapTab) => {
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
            val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, None, optInference, None, optActiveDataTab)
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

            val (dataStr, eitherRDF) = dp.getData

            val eitherResult: Either[String,IO[Response[IO]]] = for {
              rdf <- eitherRDF
              (schemaStr, eitherSchema) = sp.getSchema(Some(rdf))
              schema <- eitherSchema
            } yield {
              val (result, maybeTrigger, time) = validate(rdf, dp, schema, sp, tp)
              validateResponse(result, time, dp, sp, tp)
            }

            eitherResult.fold(e => BadRequest(s"Error: $e"),identity)
          }
        }
      }
    }

    case req@GET -> Root / "query" :?
      OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        DataFormatParam(maybeDataFormat) +&
        OptQueryParam(optQuery) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        OptActiveDataTabParam(optActiveDataTab) +&
        OptActiveQueryTabParam(optActiveQueryTab)
        => {
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
    }

    case req@POST -> Root / "query" => {
      req.decode[Multipart[IO]] { m => {
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap).value
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
                    dp.endpoint,
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
       }
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
        case Right(optDataFormat) => {
          val dp = DataParam(optData, optDataURL, None, optEndpoint, optDataFormat, optDataFormat, None, optInference, None, optActiveDataTab)
          val (dataStr, eitherRDF) = dp.getData
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
            jsonResult <- ApiHelper.shapeInfer(rdf, optNodeSelectorParam, optInference, optSchemaEngineParam, optSchemaFormatParam, None)
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
          )
        }
      }
    }

    case req@POST -> Root / "shapeInfer" => {
      req.decode[Multipart[IO]] { m => {
        val partsMap = PartsMap(m.parts)
        val r = for {
          dataPair <- DataParam.mkData(partsMap)
          (rdf, dp) = dataPair
          nodeSelector <- EitherT(partsMap.eitherPartValue("nodeSelector"))
          schemaEngine <- EitherT(partsMap.eitherPartValue("schemaEngine"))
          schemaFormat <- EitherT(partsMap.eitherPartValue("schemaFormatTextArea"))
          jsonResult <- EitherT.fromEither[IO](shapeInfer(rdf, Some(nodeSelector), dp.inference, Some(schemaEngine), Some(schemaFormat), None))
        } yield {
          val dv = DataValue(dp.data,dp.dataURL,dp.dataFormat.getOrElse(defaultDataFormat),availableDataFormats,
            dp.inference.getOrElse(defaultInference),
            availableInferenceEngines,
            dp.endpoint,
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
        r.value.unsafeRunSync().fold(e => BadRequest(s"Error: $e"),identity)
    }
   }
  }

    // Contents on /static are mapped to /static
    case r@GET -> _ if r.pathInfo.startsWith("/static") => static(r).getOrElseF(NotFound())

    // case r @ GET -> _ if r.pathInfo.startsWith("/swagger.json") => views(r)

    // When accessing to a folder (ends by /) append index.scala.html
    case r@GET -> _ if r.pathInfo.endsWith("/") =>
      webService(r.withPathInfo(r.pathInfo + "index.scala.html")).getOrElseF(NotFound())

    case r@GET -> _ =>
      val rr = if (r.pathInfo.contains('.')) r else r.withPathInfo(r.pathInfo + ".html")
      views(rr).getOrElseF(NotFound())
  }

  def err[A](str: String): Either[String, A] = {
    Left(str)
  }

  private[server] def validateResponse(result: Result,
                                       time: Long,
                                       dp: DataParam,
                                       sp: SchemaParam,
                                       tp: TriggerModeParam): IO[Response[IO]] = {
    val dv = DataValue(
      dp.data, dp.dataURL,
      dp.dataFormat.getOrElse(defaultDataFormat), availableDataFormats,
      dp.inference.getOrElse(defaultInference), availableInferenceEngines,
      dp.endpoint,
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
    
    val validationReport: Option[Either[String,String]] =
      Some(result.validationReport.flatMap(_.serialize("TURTLE")))

    logger.info(s"Validation report: $validationReport")

    Ok(html.validate(Some(result),time,
      validationReport,
      dv, sv,
      availableTriggerModes,
      tp.triggerMode.getOrElse(defaultTriggerMode),
      smv,
      getSchemaEmbedded(sp)
    ))
  }

  private def allNone(maybes: Option[String]*) = {
    maybes.forall(_.isEmpty)
  }
}