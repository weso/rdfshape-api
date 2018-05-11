package es.weso.server

import cats.implicits._
import cats.effect.IO
import es.weso.html
import es.weso.rdf.PrefixMap
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema._
import es.weso.server.Defaults._
import es.weso.utils.FileUtils
import es.weso.rdf.RDFReasoner
import io.circe._
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl.io.Ok
import org.http4s.twirl._
import Http4sUtils._
import org.http4s.multipart._
import es.weso.rdf.dot.RDF2Dot


import scala.util.Try

object ApiHelper {

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
        println(s"Error parsing $urlStr")
        Left(fail.message)
      },
      uri => Try {
        val httpClient = PooledHttp1Client[IO]()
        val resolvedUri = baseUri.resolve(uri)
        println(s"Resolved: $resolvedUri")
        httpClient.expect[String](resolvedUri).unsafeRunSync()
      }.toEither.leftMap(_.getMessage).map(Some(_))
    )
  }

  private[server] def dataConvert(optData: Option[String],
                  optDataFormat: Option[String],
                  optTargetDataFormat: Option[String]): Either[String, Option[String]] = optData match {
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
                  base: Option[String]): Either[String, Option[String]] = optSchema match {
    case None => Right(None)
    case Some(schemaStr) => {
      val schemaFormat = optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
      val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
      for {
        schema <- Schemas.fromString(schemaStr, schemaFormat, schemaEngine, base)
        result <- schema.convert(optTargetSchemaFormat,optSchemaEngine)
      } yield Some(result)
    }
  }

  private[server] def validate(data: String,
               optDataFormat: Option[String],
               optSchema: Option[String],
               optSchemaFormat: Option[String],
               optSchemaEngine: Option[String],
               tp: TriggerModeParam,
               optNode: Option[String],
               optShape: Option[String],
               // optShapeMap: Option[String],
               optInference: Option[String]
              ): (Result, Option[ValidationTrigger], Long) = {

    val startTime = System.nanoTime()

    val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
    val schemaEngine = optSchemaEngine.getOrElse(Schemas.defaultSchemaName)
    val schemaFormat = optSchema match {
      case None => dataFormat
      case Some(_) => optSchemaFormat.getOrElse(Schemas.defaultSchemaFormat)
    }

    val schemaStr = optSchema match {
      case None => data
      case Some(schema) => schema
    }

    val base = Some(FileUtils.currentFolderURL)

    Schemas.fromString(schemaStr, schemaFormat, schemaEngine, base) match {
      case Left(e) =>
        (Result.errStr(s"Error reading schema: $e\nschemaFormat: $schemaFormat, schemaEngine: $schemaEngine\nschema:\n$schemaStr"), None, 0)
      case Right(schema) => {
        RDFAsJenaModel.fromChars(data, dataFormat, base) match {
          case Left(e) =>
            (Result.errStr(s"Error reading rdf data: $e\ndataFormat: $dataFormat\nRDF Data:\n$data"), None, 0)
          case Right(rdf) => {
            rdf.applyInference(optInference.getOrElse("None")) match {
              case Left(msg) => (Result.errStr(s"Error applying inference to RDF: $msg"), None, 0)
              case Right(newRdf) => {
                val triggerMode = tp.triggerMode // optTriggerMode.getOrElse(ValidationTrigger.default.name)
                // val shapeMap = optShapeMap.getOrElse("")
                val (optShapeMapStr, eitherShapeMap) = tp.getShapeMap(rdf.getPrefixMap,schema.pm)
                ValidationTrigger.findTrigger(triggerMode.getOrElse(Defaults.defaultTriggerMode),
                  optShapeMapStr.getOrElse(""),
                  base, optNode, optShape, rdf.getPrefixMap, schema.pm) match {
                  case Left(msg) => (
                    Result.errStr(s"Cannot obtain trigger: $triggerMode\nshapeMap: $optShapeMapStr\nmsg: $msg"),
                    None, 0)
                  case Right(trigger) => {
                    val result = schema.validate(newRdf, trigger)
                    val endTime = System.nanoTime()
                    val time: Long = endTime - startTime
                    (result,Some(trigger),time)
                  }
                }
              }
            }
          }
        }
      }
    }
  }


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
    val svg: String =
      schema.serialize("SVG").fold(
        e => s"Error converting to SVG: $e",
        identity
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