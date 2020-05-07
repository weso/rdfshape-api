package es.weso.server

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.APIDefinitions._
import es.weso.server.ApiHelper._
import es.weso.server.Defaults._
import es.weso.server.QueryParams._
import es.weso.server.format._
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
import es.weso.server.ApiHelper.SchemaInfoReply
import org.log4s.getLogger
import es.weso.utils.IOUtils._


class SchemaService[F[_]: ConcurrentEffect: Timer](blocker: Blocker, client: Client[F])(implicit cs: ContextShift[F])
    extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase
  private val logger       = getLogger
  
  val L = implicitly[LiftIO[F]]

  val routes = HttpRoutes.of[F] {

    case GET -> Root / `api` / "schema" / "engines" => {
      val engines = Schemas.availableSchemaNames
      val json    = Json.fromValues(engines.map(str => Json.fromString(str)))
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "engines" / "default" => {
      val schemaEngine = Schemas.defaultSchemaName
      val json         = Json.fromString(schemaEngine)
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "formats" :?
      SchemaEngineParam(optSchemaEngine) => {
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val json = Schemas.lookupSchema(schemaEngine) match {
        case Right(schema) => Json.fromValues(schema.formats.toList.map(Json.fromString(_)))
        case Left(_) => Json.fromFields(List(("error", Json.fromString(s"Schema engine: ${schemaEngine} not found. Available engines = ${Schemas.availableSchemaNames.mkString(",")}"))))
      }
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "triggerModes" => {
      val triggerModes = ValidationTrigger.triggerValues.map(_._1)
      val json         = Json.fromValues(triggerModes.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "info" :?
          OptSchemaParam(optSchema) +&
            SchemaFormatParam(optSchemaFormat) +&
            SchemaEngineParam(optSchemaEngine) => {
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val schemaStr = optSchema match {
        case None         => ""
        case Some(schema) => schema
      }
      for {
        either <- L.liftIO(Schemas.fromString(schemaStr, schemaFormat, schemaEngine, None).value)
        r <- either.fold(
          e => errJson(s"Error reading schema: $e\nString: $schemaStr"), 
          schema => {
            val shapes: List[String] = schema.shapes
            val jsonShapes           = Json.fromValues(shapes.map(Json.fromString(_)))
            val pm: Json             = prefixMap2Json(schema.pm)
            val result               = SchemaInfoResult(schemaStr, schemaFormat, schemaEngine, jsonShapes, pm).asJson
            Ok(result)
          }
        )
      } yield r 
    }

    case req @ POST -> Root / `api` / "schema" / "info" =>
      req.decode[Multipart[F]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: EitherT[F, String, Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap,None)
            (schema, sp) = schemaPair
          } yield {
            schemaInfo(schema)
          }
          for {
            e <- r.value
            v <- e.fold(msg => {
              println(s"###### Error $msg")
              Ok(SchemaInfoReply.fromError(msg).toJson)
            }, Ok(_))
          } yield v
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
      val schemaStr = optSchema match {
        case None         => ""
        case Some(schema) => schema
      }
      for {
        either <- L.liftIO(Schemas.fromString(schemaStr, schemaFormat, schemaEngine, None).value)
        r <- either.fold(
          e => errJson(s"Error reading schema: $e\nString: $schemaStr"),
          schema => {
            for {
              s <- io2f(convertSchema(schema, optSchema, schemaFormat, schemaEngine, optResultSchemaFormat, optResultSchemaEngine))
              r <- Ok(s.toJson)
            } yield r
          } 
            // Ok(convertSchema(schema, optSchema, schemaFormat, schemaEngine, optResultSchemaFormat, optResultSchemaEngine).toJson)
        )
      } yield r 
    }

    case req @ POST -> Root / `api` / "schema" / "convert" =>
      req.decode[Multipart[F]] { m =>
      {
        val partsMap = PartsMap(m.parts)
        logger.info(s"POST info partsMap. $partsMap")
        val r: ESF[Json,F] = for {
          schemaPair <- SchemaParam.mkSchema(partsMap, None)
          (schema, sp) = schemaPair
          converted <- io2esf(convertSchema(schema, sp.schema, 
            sp.schemaFormat.getOrElse(SchemaFormat.default.name), sp.schemaEngine.getOrElse(defaultSchemaEngine), 
            sp.targetSchemaFormat, sp.targetSchemaEngine
            ))
        } yield {
          // println(s"schema / convert ---target: ${sp.targetSchemaFormat}, ${sp.targetSchemaEngine}")
          converted.toJson
        }
        for {
          e <- r.value
          v <- e.fold((s:String) => Ok(SchemaConversionResult.fromMsg(s).toJson), Ok(_))
        } yield v
      }
      }


    case req @ POST -> Root / `api` / "schema" / "visualize" =>
      req.decode[Multipart[F]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          logger.info(s"POST info partsMap. $partsMap")
          val r: EitherT[F, String, Json] = for {
            schemaPair <- SchemaParam.mkSchema(partsMap, None)
            (schema, _) = schemaPair
            v <- io2esf(schemaVisualize(schema))
          } yield {
            v
          }
          for {
            e <- r.value
            v <- e.fold(errJson(_), Ok(_))
          } yield v
        }
      }

    case req @ POST -> Root / `api` / "schema" / "cytoscape" =>
      req.decode[Multipart[F]] { m =>
      {
        val partsMap = PartsMap(m.parts)
        logger.info(s"POST info partsMap. $partsMap")
        val r: EitherT[F, String, Json] = for {
          schemaPair <- SchemaParam.mkSchema(partsMap, None)
          (schema, _) = schemaPair
        } yield {
          schemaCytoscape(schema)
        }
        for {
          e <- r.value
          v <- e.fold(errJson(_), Ok(_))
        } yield v
      }
      }

    case req @ GET -> Root / `api` / "schema" / "visualize" :?
        SchemaURLParam(optSchemaURL) +&
        OptSchemaParam(optSchema) +&
        SchemaFormatParam(optSchemaFormat) +&
        SchemaEngineParam(optSchemaEngine) +&
        OptActiveSchemaTabParam(optActiveSchemaTab) => {
      val sp = SchemaParam(optSchema,
        optSchemaURL,
        None,
        optSchemaFormat,
        optSchemaFormat,
        optSchemaFormat,
        optSchemaEngine,
        None, 
        None,
        None,
        None,
        optActiveSchemaTab)
      val r: EitherT[IO,String,String] = for {
        _ <- { println(s"#####<<<< Before...getSchema"); ok_es(())}
        pair <- EitherT(sp.getSchema(None).attempt.map(_.leftMap(s => s"Error obtaining schema: ${s.getMessage}")))
        _ <- { println(s"#####<<<< After...getSchema"); ok_es(())}
        (_,either: Either[String,Schema]) = pair
        svg <- either.fold(s => 
          fail_es(s"Error parsing schema: $s"), 
          schema => {
            io2es(schema2SVG(schema).map(_._1))
        })
      } yield svg
      for {
        either <- L.liftIO(run_es(r))
        v <- either.fold(s => errJson(s"Error obtaining schema $s"), svg => {
          Ok(svg).map(_.withContentType(`Content-Type`(MediaType.image.`svg+xml`)))
        })
      } yield v
    }

    case req @ GET -> Root / `api` / "schema" / "validate" :?
          OptDataParam(optData) +&
            OptDataURLParam(optDataURL) +&
            DataFormatParam(maybeDataFormat) +&
            CompoundDataParam(optCompoundData) +&
            OptSchemaParam(optSchema) +&
            SchemaURLParam(optSchemaURL) +&
            SchemaFormatParam(optSchemaFormat) +&
            SchemaEngineParam(optSchemaEngine) +&
            OptTriggerModeParam(optTriggerMode) +&
            ShapeMapParameterAlt(optShapeMapAlt) +&
            ShapeMapParameter(optShapeMap) +&
            ShapeMapURLParameter(optShapeMapURL) +&
            ShapeMapFileParameter(optShapeMapFile) +& // This parameter seems unnecessary...maybe for keeping the state only?
            ShapeMapFormatParam(optShapeMapFormat) +&
            SchemaEmbedded(optSchemaEmbedded) +&
            InferenceParam(optInference) +&
            OptEndpointParam(optEndpoint) +&
//            OptEndpointsParam(optEndpoints) +&
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
          val dp = DataParam(optData,
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
                             optCompoundData)
          val sp = SchemaParam(optSchema,
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
                               optActiveSchemaTab)
          val collectShapeMap = (optShapeMap, optShapeMapAlt) match {
            case (None, None)     => None
            case (None, Some(sm)) => Some(sm)
            case (Some(sm), None) => Some(sm)
            case (Some(sm1), Some(sm2)) =>
              if (sm1 == sm2) Some(sm1)
              else {
                val msg = (s"2 shape-map parameters with different values: $sm1 and $sm2. We use: $sm1")
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

          // val (dataStr, eitherRDF) = 

          val eitherResult: ESF[F[Response[F]], F] = for {
            pairData <- esio2esf(dp.getData(relativeBase))
            (dataStr, rdf) = pairData
            //rdf <- either2ef(eitherRDF)
            pair <- EitherT.liftF[F,String,(Option[String],Either[String,Schema])](L.liftIO(sp.getSchema(Some(rdf))))
            (schemaStr, eitherSchema) = pair
            schema <- EitherT.fromEither[F](eitherSchema)
            res <- io2esf(validate(rdf, dp, schema, sp, tp, relativeBase))
          } yield {
            // println(s"RDF: ${rdf.serialize("TURTLE").getOrElse("<Cannot serialize RDF>")}")
            // println(s"Schema: ${schema.serialize("ShExC",None).getOrElse("<Cannot serialize schema")}")
            val (result, maybeTrigger, time) = res
            println(s">>>> maybeTrigger: $maybeTrigger")
            println(s">>>> result: $result")
            Ok(result.toJson)
          }
          eitherResult.foldF(e => errJson(s"Error: $e"), identity)
        }
      }
    }

    case req @ POST -> Root / `api` / "schema" / "validate" =>
      req.decode[Multipart[F]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          val r: ESF[Result,F] = for {
            _ <- pp(partsMap)
            dataPair <- DataParam.mkData(partsMap, relativeBase)
            (rdf, dp) = dataPair
            _ <- pp(dp)
            schemaPair <- SchemaParam.mkSchema(partsMap, Some(rdf))
            (schema, sp) = schemaPair
            _ <- pp(sp)
            _ <- pp(schema)
            tp <- TriggerModeParam.mkTriggerModeParam(partsMap)
            _ <- pp(tp)
            res <- io2esf(validate(rdf, dp, schema, sp, tp, relativeBase))
          } yield {
            // val schemaEmbedded = getSchemaEmbedded(sp)
            // println(s"Trigger mode: $tp")
            //println(s"Schema: ${schema.serialize("ShExC").getOrElse("Error serializing schema")}")
            val (result, maybeTriggerMode, time) = res
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
    Ok(mkJsonErr(msg)) // 

  private def info(msg: String): EitherT[F,String,Unit] = 
    EitherT.liftF[F,String,Unit](LiftIO[F].liftIO(IO(println(msg))))  

  private def pp[A](v:A): EitherT[F,String,Unit] = {
    EitherT.liftF[F,String, Unit](LiftIO[F].liftIO(IO{ pprint.log(v) }))
  }

}

object SchemaService {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](blocker: Blocker, client: Client[F]): SchemaService[F] =
    new SchemaService[F](blocker, client)
}
