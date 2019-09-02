package es.weso.server

import java.util.concurrent.Executors

import cats.implicits._
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, IO, Timer}
import results._

import scala.concurrent.ExecutionContext.global
import es.weso.rdf.PrefixMap
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema._
import es.weso.server.Defaults._
import es.weso.utils.FileUtils
import es.weso.rdf.RDFReasoner
import es.weso.rdf.nodes.IRI
import io.circe._
import org.http4s._, org.http4s.dsl.io._
import org.http4s.circe._
import org.log4s.getLogger
import es.weso.uml._
import es.weso.schemaInfer._
import es.weso.server.helper.DataFormat
import es.weso.shapeMaps.NodeSelector
import org.http4s.client.{Client, JavaNetClientBuilder}
import es.weso.rdf.sgraph._

import scala.util.Try

object ApiHelper {

  private val logger = getLogger
  private val NoTime = 0L

  /**
    * Get base URI
    * @return default URI obtained from current folder
    */
  private[server] def getBase: Option[String] = Defaults.relativeBase.map(_.str)

  private[server] def prefixMap2Json(pm: PrefixMap): Json = {
    Json.fromFields(pm.pm.map { case (prefix, iri) => (prefix.str, Json.fromString(iri.getLexicalForm)) })
  }

  private[server] def resolveUri(baseUri: Uri, urlStr: String): Either[String, Option[String]] = {
    println(s"Handling Uri: $urlStr")
    // TODO: handle timeouts
    Uri.fromString(urlStr).fold(
      fail => {
        logger.info(s"Error parsing $urlStr")
        Left(fail.message)
      },
      uri => Try {
        // TODO: The following code is unsafe...
        implicit val cs: ContextShift[IO] = IO.contextShift(global)
        implicit val timer: Timer[IO] = IO.timer(global)
        val blockingPool = Executors.newFixedThreadPool(5)
        val blocker = Blocker.liftExecutorService(blockingPool)
        val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create
        val resolvedUri = baseUri.resolve(uri)
        logger.info(s"Resolved: $resolvedUri")
        httpClient.expect[String](resolvedUri).unsafeRunSync()
      }.toEither.leftMap(_.getMessage).map(Some(_))
    )
  }

/*  private[server] def dataConvert(
     optData: Option[String],
     optDataFormat: Option[String],
     optTargetDataFormat: Option[String]): Either[String, Option[String]] =
   optData match {
    case None => Right(None)
    case Some(data) => {
      val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
      val resultDataFormat = optTargetDataFormat.getOrElse(DataFormats.defaultFormatName)
      for {
        rdf <- RDFAsJenaModel.fromChars(data, dataFormat, None)
        str <- rdf.serialize(resultDataFormat)
      } yield Some(str)
    }
  } */

  private[server] def schemaConvert(optSchema: Option[String],
                  optSchemaFormat: Option[String],
                  optSchemaEngine: Option[String],
                  optTargetSchemaFormat: Option[String],
                  optTargetSchemaEngine: Option[String],
                  base: Option[String]): Either[String, Option[String]] =
   optSchema match {
    case None => Right(None)
    case Some(schemaStr) => {
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      for {
        schema <- Schemas.fromString(schemaStr, schemaFormat, schemaEngine, base)
        result <- schema.convert(optTargetSchemaFormat,optTargetSchemaEngine,base.map(IRI(_)))
      } yield Some(result)
    }
  }

  private[server] def validate(rdf: RDFReasoner,
                               dp:DataParam,
                               schema: Schema,
                               sp: SchemaParam,
                               tp: TriggerModeParam,
                               relativeBase: Option[IRI]
              ): (Result, Option[ValidationTrigger], Long) = {
    println(s"APIHelper: validate")
    val base = relativeBase.map(_.str) // Some(FileUtils.currentFolderURL)
    val triggerMode = tp.triggerMode
    val (optShapeMapStr, eitherShapeMap) = tp.getShapeMap(rdf.getPrefixMap,schema.pm)
    ValidationTrigger.findTrigger(triggerMode.getOrElse(Defaults.defaultTriggerMode),
         optShapeMapStr.getOrElse(""), base, None, None,
         rdf.getPrefixMap, schema.pm) match {
         case Left(msg) =>
            err(s"Cannot obtain trigger: $triggerMode\nshapeMap: $optShapeMapStr\nmsg: $msg")
         case Right(trigger) => {
             val startTime = System.nanoTime()
             val result = schema.validate(rdf, trigger)
             val endTime = System.nanoTime()
             val time: Long = endTime - startTime
             (result,Some(trigger),time)
           }
    }
  }

  private[server] def validateStr(data: String,
                                  optDataFormat: Option[DataFormat],
                                  optSchema: Option[String],
                                  optSchemaFormat: Option[String],
                                  optSchemaEngine: Option[String],
                                  tp: TriggerModeParam,
                                  optInference: Option[String],
                                  relativeBase: Option[IRI]
                                 ): (Result, Option[ValidationTrigger], Long) = {
    val dp = DataParam.empty.copy(
      data = Some(data),
      dataFormatTextarea = optDataFormat,
      inference = optInference
    )
    val sp = SchemaParam.empty.copy(
      schema = optSchema,
      schemaFormatTextArea = optSchemaFormat,
      schemaEngine = optSchemaEngine
    )
    val (_,eitherRDF) = dp.getData(relativeBase)
    val result = for {
     rdf <- eitherRDF
     (_,eitherSchema) = sp.getSchema(Some(rdf))
     schema <- eitherSchema
    } yield (rdf,schema)

    result.fold(
      e => err(e),
      r => {
        val (rdf,schema) = r
        validate(rdf,dp,schema,sp,tp, relativeBase)
      }
   )
  }

  private def err(msg: String) =
    (Result.errStr(s"Error: $msg"), None, NoTime )

  private[server] def query(data: String,
            optDataFormat: Option[DataFormat],
            optQuery: Option[String],
            optInference: Option[String]
           ): Either[String, Json] = {
    optQuery match {
      case None => Right(Json.Null)
      case Some(queryStr) => {
        val dataFormat = optDataFormat.getOrElse(defaultDataFormat)
        val base = Some(IRI(FileUtils.currentFolderURL))
        for {
          basicRdf <- RDFAsJenaModel.fromChars(data, dataFormat.name, base)
          rdf <- basicRdf.applyInference(optInference.getOrElse("None"))
          json <- rdf.queryAsJson(queryStr)
        } yield json
      }
    }
  }

  private[server] def shapeInfer(rdf: RDFReasoner,
                                 optNodeSelector: Option[String],
                                 optInference: Option[String],
                                 optEngine: Option[String],
                                 optSchemaFormat: Option[String],
                                 optLabelName: Option[String],
                                 relativeBase: Option[IRI]
                                ): Either[String, Json] = {
   val base = relativeBase.map(_.str)
   val engine = optEngine.getOrElse(defaultSchemaEngine)
   val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat)
   optNodeSelector match {
     case None => Right(Json.Null)
     case Some(nodeSelector) => {
       for {
         selector <- NodeSelector.fromString(nodeSelector, base, rdf.getPrefixMap())
         result <- {
           println(s"Selector: $selector")
           SchemaInfer.runInferSchema(rdf, selector, engine, optLabelName.map(IRI(_)).getOrElse(defaultShapeLabel))
         }
         (schemaInfer, resultMap) = result
         uml <- Schema2UML.schema2UML(schemaInfer)
         str <- schemaInfer.serialize(schemaFormat)
       } yield Json.fromFields(
         List(
           ("inferedShape", Json.fromString(str)),
           ("format", Json.fromString(schemaFormat)),
           ("engine", Json.fromString(engine)),
           ("svg", Json.fromString(uml.toSVG))
         )
       )
     }
   }
  }

  private[server] def dataFormatOrDefault(df: Option[String]): String =
    df.getOrElse(DataFormats.defaultFormatName)

  private[server] def dataInfoFromString(data: String, dataFormatStr: String): Json =  {
    val either = for {
      dataFormat <- DataFormat.fromString(dataFormatStr)
      rdf <- RDFAsJenaModel.fromChars(data,dataFormat.name)
    } yield dataInfo(rdf, Some(data), Some(dataFormat))
    either.fold(e => DataInfoResult.fromMsg(e).toJson, identity)
  }

  private[server] def dataInfo(rdf: RDFReasoner, data: Option[String], dataFormat: Option[DataFormat]): Json =  {
    val either = for {
      numberStatements <- rdf.getNumberOfStatements
      preds <- rdf.predicates
    } yield DataInfoResult.fromData(data, dataFormat, preds, numberStatements, rdf.getPrefixMap)
    either.fold(e => DataInfoResult.fromMsg(e), identity).toJson
  }

  private[server] def getSchema(sv: SchemaValue): Either[String,Schema] = {
    val schemaEngine = sv.currentSchemaEngine
    val schemaFormat = sv.currentSchemaFormat
    val schemaStr = sv.schema.getOrElse("")
    val base = Some(FileUtils.currentFolderURL)
    Schemas.fromString(schemaStr, schemaFormat, schemaEngine, base)
  }

  private[server] def schemaInfo(schema:Schema): Json = {
    val info = schema.info
    val fields: List[(String,Json)] =
      List(
        ("schemaName", Json.fromString(info.schemaName)),
        ("schemaEngine", Json.fromString(info.schemaEngine)),
        ("wellFormed", Json.fromBoolean(info.isWellFormed)),
        ("shapes", Json.fromValues(schema.shapes.map(Json.fromString(_)))),
        ("errors", Json.fromValues(info.errors.map(Json.fromString(_)))),
        ("parsed", Json.fromString("Parsed OK")),
      )
    Json.fromFields(fields)
  }

  private[server] def schemaVisualize(schema:Schema): Json = {
    val eitherUML = Schema2UML.schema2UML(schema)

    val (svg,plantuml):(String,String) = eitherUML.fold(
      e => (s"SVG conversion: $e", s"UML Error convertins: $e"),
      uml => {
        // println(s"UML converted: $uml")
        (uml.toSVG, uml.toPlantUML)
      })
    val info = schema.info
    val fields: List[(String,Json)] =
      List(
        ("schemaName", Json.fromString(info.schemaName)),
        ("schemaEngine", Json.fromString(info.schemaEngine)),
        ("wellFormed", Json.fromBoolean(info.isWellFormed)),
        ("errors", Json.fromValues(info.errors.map(Json.fromString(_)))),
        ("parsed", Json.fromString("Parsed OK")),
        ("svg", Json.fromString(svg)),
        ("plantUML", Json.fromString(plantuml))
      )
    Json.fromFields(fields)
  }

  private[server] def getSchemaEmbedded(sp: SchemaParam): Boolean = {
    sp.schemaEmbedded match {
      case Some(true) => true
      case Some(false) => false
      case None => defaultSchemaEmbedded
    }
  }

  // TODO: I want to move this method here but it infers type IO instead of generic F
  /* private[server] def errJson[F[_]:ConcurrentEffect](msg: String): F[Response[F]] =
    Ok(Json.fromFields(List(("error",Json.fromString(msg)))))

   */
}