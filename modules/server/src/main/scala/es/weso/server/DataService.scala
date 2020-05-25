package es.weso.server

import cats._
import cats.effect._
import cats.implicits._
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import es.weso.rdf.streams.Streams
import es.weso.schema._
import es.weso.server.ApiHelper._
import results._
import es.weso.server.Defaults.{availableDataFormats, availableInferenceEngines, defaultActiveDataTab, defaultDataFormat, defaultInference}
import es.weso.server.QueryParams._
import es.weso.server.format._
import es.weso.server.utils.Http4sUtils._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart
import org.http4s.server.staticcontent.{ResourceService, resourceService}
import org.log4s.getLogger
import scala.concurrent.duration._
import APIDefinitions._
import cats.Monad
import cats.data.EitherT
// import es.weso.html
import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.IRI
import org.http4s.dsl.io.Ok
import es.weso.utils.IOUtils._
import es.weso.server.utils.OptEitherF._

import scala.util.Try

class DataService[F[_]:ConcurrentEffect: Timer](blocker: Blocker,
                                               client: Client[F])(implicit cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase
  private val logger = getLogger

  val routes = HttpRoutes.of[F] {

    // Input RDF data formats include html-microdata, turtle, json-ld...
    case GET -> Root / `api` / "data" / "formats" / "input" => {
      val formats = DataFormat.availableFormats.map(_.name)
      val json = Json.fromValues(formats.map(Json.fromString(_)))
      Ok(json)
    }

    // Output RDF data conversion formats
    case GET -> Root / `api` / "data" / "formats" / "output" => {
      val formats = DataFormats.availableFormats.map(_.name)
      val json = Json.fromValues(formats.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "data" / "formats" / "default" => {
      val dataFormat = DataFormat.default.name
      Ok(Json.fromString(dataFormat))
    }

    case GET -> Root / `api` / "data" / "inferenceEngines" => {
      val inferenceEngines = Defaults.availableInferenceEngines
      val json = Json.fromValues(inferenceEngines.map(Json.fromString(_)))
      Ok(json)
    }

    case GET -> Root / `api` / "data" / "inferenceEngines" / "default" => {
      val defaultInferenceEngine = Defaults.defaultInference
      Ok(Json.fromString(defaultInferenceEngine))
    }

    case GET -> Root / `api` / "data" / "visualize" / "formats" => {
      val formats = DataConverter.availableGraphFormatNames ++
        List(
          "DOT", // DOT is not a visual format but can be used to debug
          "JSON" // JSON is the format that can be used by Cytoscape
        )
      val json = Json.fromValues(formats.map(Json.fromString(_)))
      Ok(json)
    }

    case req@GET -> Root / `api` / "dataUrl" / "info" :?
      OptDataURLParam(optDataUrl) +&
        DataFormatParam(optDataFormat) => {
      val dataFormat = dataFormatOrDefault(optDataFormat)
      optDataUrl match {
        case None => errJson(s"Must provide a dataUrl")
        case Some(dataUrl) => for {
           data <- client.expect[String](dataUrl)
           result <- io2f(dataInfoFromString(data, dataFormat))
           r <- Ok(result).map(_.withContentType(`Content-Type`(MediaType.application.json)))
        } yield r
      }
    }

    case req@POST -> Root / `api` / "data" / "info" => {
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          _ <- { pprint.log(maybeData); Monad[F].pure(()) } 
          response <- maybeData match {
            case Left(err) => errJson(
              s"""|Error obtaining RDF data
                  |$err""".stripMargin
            )
            case Right((rdf, dp)) => {
              val dataFormat = dataFormatOrDefault(dp.dataFormat.map(_.name))
              dp.data match {
                case Some(data) => for {
                  r <- io2f(dataInfoFromString(data, dataFormat))
                  ok <- Ok(r)
                } yield ok
                case None => for {
                  str <- io2f(rdf.serialize("TURTLE"))
                  ok <- Ok(DataInfoResult.fromMsg(s"No data, but RDF=${str}").toJson)
                } yield ok 
              }
            }
          }
          _ <- { pprint.log(response); Monad[F].pure(()) } 
        } yield response
      }
    }

    case req@GET -> Root / `api` / "data" / "info" :?
      OptDataParam(optData) +&
      OptDataURLParam(optDataURL) +&
      CompoundDataParam(optCompoundData) +&
      DataFormatParam(maybeDataFormat) +&
      InferenceParam(optInference) +&
      OptEndpointParam(optEndpoint) +&
      OptActiveDataTabParam(optActiveDataTab) => {

      val either: Either[String, Option[DataFormat]] = for {
        df <- maybeDataFormat.map(DataFormat.fromString(_)).sequence
      } yield df

      either match {
        case Left(str) => errJson(str)
        case Right(optDataFormat) => {
          val dp =
            DataParam(optData, optDataURL, None, optEndpoint,
              optDataFormat, optDataFormat, optDataFormat,
              None, //no dataFormatFile
              optInference,
              None, optActiveDataTab,
              optCompoundData)
          for {
            eitherData <- io2f(dp.getData(relativeBase).value)
            r <- eitherData.fold(err => errJson(err), pair  => { 
              val (maybeStr,rdf) = pair
              for {
                s <- io2f(dataInfo(rdf, maybeStr, optDataFormat))
                ok <- Ok(s)
              } yield ok
            })
          } yield r
        }
      }
    }

    case req @ POST -> Root / `api` / "data" / "convert" => {
      println(s"POST /api/data/convert, Request: $req")
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(msg) => errJson(s"Error obtaining data: $msg")
            case Right((rdf, dp)) => {
              val targetFormat = dp.targetDataFormat.getOrElse(defaultDataFormat).name
              val dataFormat = dp.dataFormat.getOrElse(defaultDataFormat)
              pprint.log(dp)
              pprint.log(dataFormat)
              for {
                result <- io2f(DataConverter.rdfConvert(rdf, dp.data, dataFormat, targetFormat))
                ok <- Ok(result.toJson)
              } yield ok
            }
          }
        } yield response
      }
    }

    case req @ GET -> Root / `api` / "data" / "convert" :?
      DataParameter(data) +&
      DataFormatParam(optDataFormat) +&
      CompoundDataParam(optCompoundData) +&
      TargetDataFormatParam(optResultDataFormat) => {
      for {
        eitherDataFormat <- either2ef[DataFormat,F](DataFormat.fromString(optDataFormat.getOrElse(defaultDataFormat.name))).value
        result <- eitherDataFormat.fold(
          e => BadRequest(e),
          dataFormat => for {
            r <- io2f(DataConverter.dataConvert(data,dataFormat,optCompoundData,optResultDataFormat.getOrElse(defaultDataFormat.name)))
            ok <- Ok(r.toJson)
          } yield ok
        )
      } yield result
    }

    case req @ POST -> Root / `api` / "data" / "query" => {
      println(s"POST /api/data/query, Request: $req")
      req.decode[Multipart[F]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData <- DataParam.mkData(partsMap, relativeBase).value
          response <- maybeData match {
            case Left(err) => errJson(s"Error obtaining RDF data $err")
            case Right((rdf, dp)) => 
             for {
               maybePair <- SparqlQueryParam.mkQuery(partsMap)
               resp <- maybePair match {
                case Left(err) => errJson(s"Error obtaining Query data $err")
                case Right((queryStr,qp)) => {
                  pprint.log(qp);
                  val optQueryStr = qp.query.map(_.str)
                  for {
                   json <- io2f(rdf.queryAsJson(optQueryStr.getOrElse("")))
                   _ <- io2f (IO { pprint.log(json) } )
                   v <- Ok(json)
                  } yield v
                }
               }
             } yield resp
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
          schemaEngine <- partsMap.optPartValue("schemaEngine")
          optSchemaFormatStr <- partsMap.optPartValue("schemaFormat")
          inference <- partsMap.optPartValue("inference")
          label <- partsMap.optPartValue("labelName")
          optBaseStr <- partsMap.optPartValue("base")
          nodeSelector <- partsMap.optPartValue("nodeSelector")
          schemaFormat <- optEither2f(optSchemaFormatStr, SchemaFormat.fromString)
          response <- maybeData match {
            case Left(err) => for {
              res <- io2f(DataExtractResult.fromMsg(s"Error obtaining data: $err").toJson)
              ok <- Ok(res)
            } yield ok 
            // Ok(DataExtractResult.fromMsg(s"Error obtaining data: $err").toJson)
            case Right((rdf, dp)) => for {
              d <- io2f(dataExtract(rdf, dp.data, dp.dataFormatValue, nodeSelector, inference, schemaEngine, schemaFormat, label, None))
              json <- io2f(d.toJson)
              ok <- Ok(json)
             } yield ok
          }
        } yield response
      }
    }

  }

  private def parseInt(s: String): Either[String, Int] =
    Try(s.toInt).map(Right(_)).getOrElse(Left(s"$s is not a number"))

  private def errJson(msg: String): F[Response[F]] =
    Ok(Json.fromFields(List(("error",Json.fromString(msg)))))

}

object DataService {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](blocker: Blocker, client: Client[F]): DataService[F] =
    new DataService[F](blocker, client)
}
