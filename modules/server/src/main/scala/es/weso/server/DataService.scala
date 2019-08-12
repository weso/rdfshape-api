package es.weso.server

import cats.effect._
import es.weso._
import es.weso.schema._
import es.weso.server.QueryParams._
import org.http4s._
import org.http4s.multipart._
import org.http4s.twirl._
import org.http4s.implicits._
import cats.implicits._
import es.weso.server.ApiHelper._
import es.weso.server.Defaults._
import es.weso.server.helper.{DataFormat, Svg}
import io.circe.Json
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.log4s.getLogger

class DataService[F[_]](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase
  private val logger = getLogger

  val routes = HttpRoutes.of[F] {

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
          val (maybeStr, eitherRDF) = dp.getData(relativeBase)
          println(s"GET dataConversions: $maybeStr\nEitherRDF:${eitherRDF}\ndp: ${dp}\ndv: $dv")
          val result: Option[Json] =
            if (allNone(optData,optDataURL,optEndpoint))
            None
            else
             Some(
              Json.fromFields(List(
                ("href", Json.fromString(
                  uri"/api/data/convert".
                  withOptionQueryParam(QueryParams.data,dp.data).
                  withOptionQueryParam(QueryParams.dataFormat, dp.dataFormat.map(_.name)).
                  withOptionQueryParam(QueryParams.targetDataFormat, dp.targetDataFormat.map(_.name)).renderString)
                )
              )))

          Ok(html.dataConversions(result, dv,optTargetDataFormat.getOrElse(defaultDataFormat)))
        }
      }
    }

    case req@POST -> Root / "dataConversions" => {
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
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
              val result: Option[Json] =
                  Some(
                    Json.fromFields(List(
                      ("href", Json.fromString(
                        uri"/api/data/convert".
                          withOptionQueryParam(QueryParams.data,dp.data).
                          withOptionQueryParam(QueryParams.dataFormat, dp.dataFormat.map(_.name)).
                          withOptionQueryParam(QueryParams.targetDataFormat, dp.targetDataFormat.map(_.name)).renderString)
                      )
                    )))
              Ok(html.dataConversions(result,dv,
                  dp.targetDataFormat.getOrElse(defaultDataFormat)))
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
          val (maybeStr, eitherRDF) = dp.getData(relativeBase)
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
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap,relativeBase).value
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

        val either: Either[String, (Option[DataFormat], Option[DataFormat])] = for {
          df  <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
          tdf <- maybeTargetDataFormat.map(DataFormat.fromString(_)).sequence
        } yield (df, tdf)

        either match {
          case Left(str) => BadRequest(str)
          case Right(values) => {
            val (optDataFormat, optTargetDataFormat) = values
            val targetDataFormat = optTargetDataFormat.getOrElse(Svg)
            if (allNone(optData, optDataURL, optEndpoint)) {
              val dv = DataValue(
                optData,
                optDataURL,
                optDataFormat.getOrElse(defaultDataFormat),
                availableDataFormats,
                optInference.getOrElse(defaultInference),
                availableInferenceEngines,
                optEndpoint,
                optActiveDataTab.getOrElse(defaultActiveDataTab)
              )
              Ok(html.dataVisualization(dv, targetDataFormat))
            } else {
            val dp = DataParam(optData,
                               optDataURL,
                               None,
                               optEndpoint,
                               optDataFormat,
                               optDataFormat,
                               None,
                               optInference,
                               None,
                               optActiveDataTab)
            val (maybeStr, eitherRDF) = dp.getData(relativeBase)
            eitherRDF.fold(
              str => BadRequest(str),
              rdf => {
                DataConverter
                  .rdfConvert(rdf, targetDataFormat.name)
                  .fold(
                    e => BadRequest(s"Error in conversion to $targetDataFormat: $e\nRDF:\n${rdf.serialize("TURTLE")}"),
                    result => Ok(result).map(_.withContentType(`Content-Type`(targetDataFormat.mimeType)))
                  )

              }
            )
          }
        }
      }
    }


    case req@POST -> Root / "dataVisualization" => {
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap,relativeBase).value
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

  }

  def err[A](str: String): Either[String, A] = {
    Left(str)
  }

  private[server] def validateResponse(result: Result,
                                       time: Long,
                                       dp: DataParam,
                                       sp: SchemaParam,
                                       tp: TriggerModeParam): F[Response[F]] = {
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

object DataService {
  def apply[F[_]: Effect: ContextShift](blocker: Blocker): DataService[F] =
    new DataService[F](blocker)
}
