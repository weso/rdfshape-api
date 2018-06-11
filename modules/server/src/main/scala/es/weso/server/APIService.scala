package es.weso.server

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, _}
import org.http4s.dsl.io._
import org.http4s.client.blaze._
import org.http4s.server.staticcontent.ResourceService.Config
import cats.effect._
import org.http4s._
import org.http4s.headers._
import org.http4s.circe._
import org.http4s.MediaType._
import org.log4s.getLogger
import QueryParams._
import Http4sUtils._
import ApiHelper._
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.{Graph, MutableGraph}
import guru.nidi.graphviz.parse.Parser

import scala.util.Try

object APIService {

  private val logger = getLogger
  val api = "api"

  private val swagger: HttpService[IO] = staticResource(Config("/swagger", "/swagger"))


  val availableDataFormats = DataFormats.formatNames.toList
  val defaultDataFormat = DataFormats.defaultFormatName
  val availableSchemaFormats = Schemas.availableFormats
  val defaultSchemaFormat = Schemas.defaultSchemaFormat
  val availableSchemaEngines = Schemas.availableSchemaNames
  val defaultSchemaEngine = Schemas.defaultSchemaName
  val availableTriggerModes = Schemas.availableTriggerModes
  val defaultTriggerMode = Schemas.defaultTriggerMode
  val defaultSchemaEmbedded = false

  val apiService: HttpService[IO] = HttpService[IO] {

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
      val json = Json.fromValues(formats.map(str => Json.fromString(str)))
      Ok(json)
    }

    case GET -> Root / `api` / "schema" / "engines" / "default" => {
      val schemaEngine = Schemas.defaultSchemaName
      Ok(Json.fromString(schemaEngine))
    }

    case req @ GET -> Root / `api` / "test" :? NameParam(name) => {
      val default = Ok(s"Hello ${name.getOrElse("World")}")
      req.headers.get(`Accept-Language`) match {
        case Some(al) => {
          al match {
            case _ if (al.satisfiedBy(LanguageTag("es"))) =>
              Ok(s"Hola ${name.getOrElse("Mundo")}!")
            case _ => default
          }
        }
        case None => default
      }
    }

    case req @ GET -> Root / `api` / "dataUrl" / "info" :?
      OptDataURLParam(optDataUrl) +&
      DataFormatParam(optDataFormat) => {
      val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
      val httpClient = Http1Client[IO]().unsafeRunSync
      optDataUrl match {
        case None => BadRequest(s"Must provide a dataUrl")
        case Some(dataUrl) => {
          httpClient.expect[String](dataUrl).flatMap(data => {
            RDFAsJenaModel.fromChars(data, dataFormat, None) match {
              case Left(e) => BadRequest(s"Error reading rdf: $e\nRdf string: $data")
              case Right(rdf) => {
                val nodes: List[String] =
                  (
                    rdf.subjects() ++
                      rdf.iriObjects() ++
                      rdf.predicates()).map(_.toString).toList
                val jsonNodes: Json = Json.fromValues(nodes.map(str => Json.fromString(str)))
                val pm: Json = prefixMap2Json(rdf.getPrefixMap)
                val result = DataInfoResult(data, dataFormat, jsonNodes, pm).asJson
                Ok(result).map(_.withContentType(`Content-Type`(`application/json`)))
              }
          }
          })
        }
      }
    }

    case req @ GET -> Root / `api` / "data" / "info" :?
      DataParameter(data) +&
      DataFormatParam(optDataFormat) => {
      val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
      RDFAsJenaModel.fromChars(data, dataFormat, None) match {
        case Left(e) => BadRequest(s"Error reading rdf: $e\nRdf string: $data")
        case Right(rdf) => {
          val nodes: List[String] =
            (
              rdf.subjects() ++
              rdf.iriObjects() ++
              rdf.predicates()).map(_.toString).toList
          val jsonNodes: Json = Json.fromValues(nodes.map(str => Json.fromString(str)))
          val pm: Json = prefixMap2Json(rdf.getPrefixMap)
          val result = DataInfoResult(data, dataFormat, jsonNodes, pm).asJson
          Ok(result).map(_.withContentType(`Content-Type`(`application/json`)))
        }
      }
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
        case Left(e) => BadRequest(s"Error reading schema: $e\nString: $schemaStr")
        case Right(schema) => {
          val shapes: List[String] = schema.shapes
          val jsonShapes = Json.fromValues(shapes.map(Json.fromString(_)))
          val pm: Json = prefixMap2Json(schema.pm)
          //          implicit val encoder: EntityEncoder[SchemaInfoResult] = ???
          val result = SchemaInfoResult(schemaStr, schemaFormat, schemaEngine, jsonShapes, pm).asJson
          Ok(result).map(_.withContentType(`Content-Type`(`application/json`)))
        }
      }
    }

    case req @ GET -> Root / `api` / "data" / "convert" :?
      DataParameter(data) +&
      DataFormatParam(optDataFormat) +&
      TargetDataFormatParam(optResultDataFormat) => {
      dataConvert(data,optDataFormat,optResultDataFormat).fold(
        e => BadRequest(s"Error: $e"),
        result => result.resultFormat match {
          case "SVG" => {
            Ok(result.result).map(_.withContentType(`Content-Type`(`image/svg+xml`)))
          }
          case "PNG" => Ok(result.result).map(_.withContentType(`Content-Type`(`image/png`)))
          case _ => {
          val default = Ok(result.asJson)
            .map(_.withContentType(`Content-Type`(`application/json`)))
          req.headers.get(`Accept`) match {
            case Some(ah) => {
              logger.info(s"Accept header: $ah")
              val hasHTML: Boolean = ah.values.exists(mr => mr.mediaRange.satisfiedBy(`text/html`))
              if (hasHTML) {
                Ok(result.toHTML).map(_.withContentType(`Content-Type`(`text/html`)))
              } else default
            }
            case None => default
          }
        }
       }
      )
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
        case Left(e) => BadRequest(s"Error reading schema: $e\nString: $schemaStr")
        case Right(schema) => {
          if (schemaEngine.toUpperCase == resultSchemaEngine.toUpperCase) {
            schema.serialize(resultSchemaFormat) match {
              case Right(resultStr) => {
                val result = SchemaConversionResult(schemaStr, schemaFormat, schemaEngine,
                  resultSchemaFormat, resultSchemaEngine, resultStr)
                val default = Ok(result.asJson)
                  .map(_.withContentType(`Content-Type`(`application/json`)))
                req.headers.get(`Accept`) match {
                  case Some(ah) => {
                    logger.info(s"Accept header: $ah")
                    val hasHTML: Boolean = ah.values.exists(mr => mr.mediaRange.satisfiedBy(`text/html`))
                    if (hasHTML) {
                      Ok(result.toHTML).map(_.withContentType(`Content-Type`(`text/html`)))
                    } else default
                  }
                  case None => default
                }
              }
              case Left(e) =>
                BadRequest(s"Error serializing $schemaStr with $resultSchemaFormat/$resultSchemaEngine: $e")
            }
          } else {
            BadRequest(s"Conversion between different schema engines not implemented yet: $schemaEngine/$resultSchemaEngine")
          }
        }
      }
    }

    case req @ (GET | POST) -> Root / `api` / "validate" :?
      DataParameter(data) +&
      DataFormatParam(optDataFormat) +&
      OptSchemaParam(optSchema) +&
      SchemaFormatParam(optSchemaFormat) +&
      SchemaEngineParam(optSchemaEngine) +&
      OptTriggerModeParam(optTriggerMode) +&
      ShapeMapParameter(optShapeMap) +&
      ShapeMapURLParameter(optShapeMapURL) +&
      ShapeMapFileParameter(optShapeMapFile) +&
      ShapeMapFormatParam(optShapeMapFormat) +&
      OptActiveShapeMapTabParam(optActiveShapeMapTab) +&
      InferenceParam(optInference) => {
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
      val result = validateStr(data, optDataFormat,
        optSchema, optSchemaFormat, optSchemaEngine,
        tp, optInference)
      Ok(result._1.toJson)
    }

    // Contents on /swagger are directly mapped to /swagger
    case r @ GET -> _ if r.pathInfo.startsWith("/swagger/") => swagger(r).getOrElseF(NotFound())

  }

  lazy val dataFormats = RDFAsJenaModel.availableFormats.map(_.toUpperCase)
  lazy val availableGraphFormats = List(
    GraphFormat("SVG","application/svg",Format.SVG),
    GraphFormat("PNG","application/png",Format.PNG),
    GraphFormat("PS","application/ps",Format.PS)
  )
  lazy val availableFormats = availableGraphFormats.map(_.name)

  case class GraphFormat(name: String, mime: String, fmt: Format)


  private def dataConvert(data: String,
                          optDataFormat: Option[String],
                          optTargetFormat: Option[String]
                         ): Either[String,DataConversionResult] = {
    val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
    val targetFormat = optTargetFormat.getOrElse(DataFormats.defaultFormatName)
    logger.info(s"Converting $data with format $dataFormat to $targetFormat")
    for {
      rdf <- RDFAsJenaModel.fromChars(data,dataFormat,None)
      result <- targetFormat.toUpperCase match {
        case t if dataFormats.contains(t) => rdf.serialize(t)
        case t if availableFormats.contains(t) => for {
          fmt <- getTargetFormat(t)
          dot <- rdf.serialize("DOT")
          outstr <- dotConverter(dot,fmt)
        } yield outstr
        case _ => Left(s"Unsupported conversion to $targetFormat yet. Available formats: ${rdf.availableSerializeFormats.mkString(",")}")
      }
    } yield DataConversionResult(data,dataFormat,targetFormat,result)
  }

  private[server] def dotConverter(dot: String, targetFormat: Format): Either[String,String] = {
    logger.info(s"dotConverter to $targetFormat. dot\n$dot")
    println(s"targetFormat: $targetFormat")
    Try {
      val g: MutableGraph = Parser.read(dot)
      val renderer = Graphviz.fromGraph(g) //.width(200)
        .render(targetFormat)
      renderer.toString
    }.fold(
      e => Left(e.getMessage),
      s => Right(s)
    )
  }

  private def getTargetFormat(str: String): Either[String,Format] = str.toUpperCase match {
    case "SVG" => Right(Format.SVG)
    case "PNG" => Right(Format.PNG)
    case "PS" => Right(Format.PS)
    case _ => Left(s"Unsupported format $str")
  }


}