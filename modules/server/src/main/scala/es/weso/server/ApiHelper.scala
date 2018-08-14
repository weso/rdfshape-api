package es.weso.server

import cats.implicits._
import cats.effect.IO
import org.http4s.client.blaze.Http1Client
import es.weso.rdf.PrefixMap
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema._
import es.weso.server.Defaults._
import es.weso.utils.FileUtils
import es.weso.rdf.RDFReasoner
import io.circe._
import org.http4s._
import es.weso.rdf.dot.RDF2Dot
import org.log4s.getLogger
import es.weso.uml._


import scala.util.Try

object ApiHelper {

  private val logger = getLogger
  private val NoTime = 0L

  /**
    * Get base URI
    * @return default URI obtained from current folder
    */
  private[server] def getBase: Option[String] = Some(FileUtils.currentFolderURL)

  private[server] def prefixMap2Json(pm: PrefixMap): Json = {
    Json.fromFields(pm.pm.map { case (prefix, iri) => (prefix.str, Json.fromString(iri.getLexicalForm)) })
  }

  private[server] def resolveUri(baseUri: Uri, urlStr: String): Either[String, Option[String]] = {
    // TODO: handle timeouts
    Uri.fromString(urlStr).fold(
      fail => {
        logger.info(s"Error parsing $urlStr")
        Left(fail.message)
      },
      uri => Try {
        val httpClient = Http1Client[IO]().unsafeRunSync
        val resolvedUri = baseUri.resolve(uri)
        logger.info(s"Resolved: $resolvedUri")
        httpClient.expect[String](resolvedUri).unsafeRunSync()
      }.toEither.leftMap(_.getMessage).map(Some(_))
    )
  }

  private[server] def dataConvert(
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
  }

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
        result <- schema.convert(optTargetSchemaFormat,optTargetSchemaEngine)
      } yield Some(result)
    }
  }

  private[server] def validate(rdf: RDFReasoner,
                               dp:DataParam,
                               schema: Schema,
                               sp: SchemaParam,
                               tp: TriggerModeParam
              ): (Result, Option[ValidationTrigger], Long) = {
    logger.debug(s"APIHelper: validate")
    val base = Some(FileUtils.currentFolderURL)
    val triggerMode = tp.triggerMode
    val (optShapeMapStr, eitherShapeMap) = tp.getShapeMap(rdf.getPrefixMap,schema.pm)
    ValidationTrigger.findTrigger(triggerMode.getOrElse(Defaults.defaultTriggerMode),
         optShapeMapStr.getOrElse(""),base, None, None,
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
                                  optDataFormat: Option[String],
                                  optSchema: Option[String],
                                  optSchemaFormat: Option[String],
                                  optSchemaEngine: Option[String],
                                  tp: TriggerModeParam,
                                  optInference: Option[String]
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
    val (_,eitherRDF) = dp.getData
    val result = for {
     rdf <- eitherRDF
     (_,eitherSchema) = sp.getSchema(Some(rdf))
     schema <- eitherSchema
    } yield (rdf,schema)

    result.fold(e => err(e), { case (rdf,schema) =>
      validate(rdf,dp,schema,sp,tp)}
    )
  }

  private def err(msg: String) =
    (Result.errStr(s"Error: $msg"),
      None, NoTime
    )

  private[server] def query(data: String,
            optDataFormat: Option[String],
            optQuery: Option[String],
            optInference: Option[String]
           ): Either[String, Json] = {
    optQuery match {
      case None => Right(Json.Null)
      case Some(queryStr) => {
        val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
        val base = Some(FileUtils.currentFolderURL)
        for {
          basicRdf <- RDFAsJenaModel.fromChars(data, dataFormat, base)
          rdf <- basicRdf.applyInference(optInference.getOrElse("None"))
          json <- rdf.queryAsJson(queryStr)
        } yield json
      }
    }

  }

  private[server] def dataInfo(rdf: RDFReasoner): Option[Json] = {
    Some(Json.fromFields(
      List(
        ("statements", Json.fromString(rdf.getNumberOfStatements().fold(identity,_.toString))),
        ("dot",Json.fromString(RDF2Dot.rdf2dot(rdf).toString)),
        ("nodesPrefixMap", ApiHelper.prefixMap2Json(rdf.getPrefixMap()))
      )
    ))
  }

  private[server] def getSchema(sv: SchemaValue): Either[String,Schema] = {
    val schemaEngine = sv.currentSchemaEngine
    val schemaFormat = sv.currentSchemaFormat
    val schemaStr = sv.schema.getOrElse("")
    val base = Some(FileUtils.currentFolderURL)
    Schemas.fromString(schemaStr, schemaFormat, schemaEngine, base)
  }

  private[server] def schemaInfo(schema:Schema): Json = {
    val svg: String = Schema2UML.schema2UML(schema).fold(
        e => s"SVG conversion: $e",
        uml => {
          println(s"UML converted: $uml")
          uml.toSVG
        }
      )
    val info = schema.info
    val fields: List[(String,Json)] =
      List(
        ("schemaName", Json.fromString(info.schemaName)),
        ("schemaEngine", Json.fromString(info.schemaEngine)),
        ("wellFormed", Json.fromBoolean(info.isWellFormed)),
        ("errors", Json.fromValues(info.errors.map(Json.fromString(_)))),
        ("parsed", Json.fromString("Parsed OK")),
        ("svg", Json.fromString(svg))
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


}