package es.weso.rdfshape.server.server

import cats.effect._
import cats.implicits._
import es.weso.schema._
import es.weso.rdfshape.server.server.APIDefinitions._
import es.weso.rdfshape.server.server.ApiHelper._
import es.weso.rdfshape.server.server.Defaults.defaultDataFormat
import es.weso.rdfshape.server.server.QueryParams._
import es.weso.rdfshape.server.server.format._
import es.weso.rdfshape.server.server.results._
import es.weso.rdfshape.server.server.utils.OptEitherF._
import es.weso.utils.IOUtils._
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.multipart.Multipart
import org.log4s.getLogger

import scala.util.Try

class DataService(client: Client[IO]) extends Http4sDsl[IO] {

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Input RDF data formats include html-microdata, turtle, json-ld...
    case GET -> Root / `api` / "data" / "formats" / "input" =>
      val formats = DataFormat.availableFormats.map(_.name)
      val json    = Json.fromValues(formats.map(Json.fromString))
      Ok(json)

    // Output RDF data conversion formats
    case GET -> Root / `api` / "data" / "formats" / "output" =>
      val formats = DataFormats.availableFormats.map(_.name)
      val json    = Json.fromValues(formats.map(Json.fromString))
      Ok(json)

    case GET -> Root / `api` / "data" / "formats" / "default" =>
      val dataFormat = DataFormat.default.name
      Ok(Json.fromString(dataFormat))

    case GET -> Root / `api` / "data" / "inferenceEngines" =>
      val inferenceEngines = Defaults.availableInferenceEngines
      val json             = Json.fromValues(inferenceEngines.map(Json.fromString))
      Ok(json)

    case GET -> Root / `api` / "data" / "inferenceEngines" / "default" =>
      val defaultInferenceEngine = Defaults.defaultInference
      Ok(Json.fromString(defaultInferenceEngine))

    case GET -> Root / `api` / "data" / "visualize" / "formats" =>
      val formats = DataConverter.availableGraphFormatNames ++
        List(
          "DOT", // DOT is not a visual format but can be used to debug
          "JSON" // JSON is the format that can be used by Cytoscape
        )
      val json = Json.fromValues(formats.map(Json.fromString))
      Ok(json)

    case req @ GET -> Root / `api` / "dataUrl" / "info" :?
        OptDataURLParam(optDataUrl) +&
        DataFormatParam(optDataFormat) =>
      val dataFormat = dataFormatOrDefault(optDataFormat)
      optDataUrl match {
        case None => errJson(s"Must provide a dataUrl")
        case Some(dataUrl) =>
          for {
            data   <- client.expect[String](dataUrl)
            result <- io2f(dataInfoFromString(data, dataFormat))
            r <- Ok(result).map(
              _.withContentType(`Content-Type`(MediaType.application.json))
            )
          } yield r
      }

    case req @ POST -> Root / `api` / "data" / "info" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          dataParam <- DataParam.mkData(partsMap, relativeBase)
          (resource, dp) = dataParam
          dataFormat     = dataFormatOrDefault(dp.dataFormat.map(_.name))
          response <- dp.data match {
            case Some(data) =>
              for {
                r  <- dataInfoFromString(data, dataFormat)
                ok <- Ok(r)
              } yield ok
            case None =>
              val resp: IO[Json] =
                resource.use(rdf => dataInfo(rdf, None, dp.dataFormat))
              val x: IO[Response[IO]] = for {
                json <- resp
                ok   <- Ok(json)
              } yield ok
              x
          }
        } yield response
      }

    case req @ GET -> Root / `api` / "data" / "info" :?
        OptDataParam(optData) +&
        OptDataURLParam(optDataURL) +&
        CompoundDataParam(optCompoundData) +&
        DataFormatParam(maybeDataFormat) +&
        InferenceParam(optInference) +&
        OptEndpointParam(optEndpoint) +&
        OptActiveDataTabParam(optActiveDataTab) =>
      val either: Either[String, Option[DataFormat]] = for {
        df <- maybeDataFormat.map(DataFormat.fromString).sequence
      } yield df

      val r: IO[Response[IO]] = either.fold(
        str => errJson(str),
        optDataFormat => {
          val dp =
            DataParam(
              optData,
              optDataURL,
              None,
              optEndpoint,
              optDataFormat,
              optDataFormat,
              optDataFormat,
              None, //no dataFormatFile
              optInference,
              None,
              optActiveDataTab,
              optCompoundData
            )
          for {
            dataParam <- io2f(dp.getData(relativeBase))
            (maybeStr, res) = dataParam
            json <- io2f(res.use(rdf => dataInfo(rdf, maybeStr, optDataFormat)))
            ok   <- Ok(json)
          } yield ok
        }
      )
      r

    case req @ POST -> Root / `api` / "data" / "convert" =>
      println(s"POST /api/data/convert, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          dataParam <- DataParam.mkData(partsMap, relativeBase)
          (resourceRdf, dp) = dataParam
          targetFormat      = dp.targetDataFormat.getOrElse(defaultDataFormat).name
          dataFormat        = dp.dataFormat.getOrElse(defaultDataFormat)
          result <- io2f(
            resourceRdf.use(rdf => {
              pprint.log(dp)
              DataConverter.rdfConvert(rdf, dp.data, dataFormat, targetFormat)
            })
          )
          ok <- Ok(result.toJson)
        } yield ok
      }

    case req @ GET -> Root / `api` / "data" / "convert" :?
        DataParameter(data) +&
        DataFormatParam(optDataFormat) +&
        CompoundDataParam(optCompoundData) +&
        TargetDataFormatParam(optResultDataFormat) =>
      for {
        eitherDataFormat <- either2ef[DataFormat, IO](
          DataFormat.fromString(optDataFormat.getOrElse(defaultDataFormat.name))
        ).value
        result <- eitherDataFormat.fold(
          e => BadRequest(e),
          dataFormat =>
            for {
              r <- io2f(
                DataConverter.dataConvert(
                  data,
                  dataFormat,
                  optCompoundData,
                  optResultDataFormat.getOrElse(defaultDataFormat.name)
                )
              )
              ok <- Ok(r.toJson)
            } yield ok
        )
      } yield result

    case req @ POST -> Root / `api` / "data" / "query" =>
      println(s"POST /api/data/query, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        pprint.log(partsMap)
        for {
          dataParam <- DataParam.mkData(partsMap, relativeBase)
          (resourceRdf, dp) = dataParam
          maybePair <- SparqlQueryParam.mkQuery(partsMap)
          resp <- maybePair match {
            case Left(err) => errJson(s"Error obtaining Query data $err")
            case Right((queryStr, qp)) =>
              val optQueryStr = qp.query.map(_.str)
              pprint.log(optQueryStr)
              for {
                json <- io2f(
                  resourceRdf.use(rdf =>
                    rdf.queryAsJson(optQueryStr.getOrElse(""))
                  )
                )
                v <- Ok(json)
              } yield v
          }
        } yield resp
      }

    case req @ POST -> Root / `api` / "data" / "extract" =>
      println(s"POST /api/data/extract, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        for {
          maybeData          <- DataParam.mkData(partsMap, relativeBase).attempt
          schemaEngine       <- partsMap.optPartValue("schemaEngine")
          optSchemaFormatStr <- partsMap.optPartValue("schemaFormat")
          inference          <- partsMap.optPartValue("inference")
          label              <- partsMap.optPartValue("labelName")
          optBaseStr         <- partsMap.optPartValue("base")
          nodeSelector       <- partsMap.optPartValue("nodeSelector")
          schemaFormat <- optEither2f(
            optSchemaFormatStr,
            SchemaFormat.fromString
          )
          response <- maybeData match {
            case Left(err) =>
              for {
                res <- io2f(
                  DataExtractResult
                    .fromMsg(s"Error obtaining data: ${err.getMessage}")
                    .toJson
                )
                ok <- Ok(res)
              } yield ok
            /* Ok(DataExtractResult.fromMsg(s"Error obtaining data:
             * $err").toJson) */
            case Right((resourceRdf, dp)) =>
              for {
                d <- io2f(
                  resourceRdf.use(rdf =>
                    dataExtract(
                      rdf,
                      dp.data,
                      dp.dataFormatValue,
                      nodeSelector,
                      inference,
                      schemaEngine,
                      schemaFormat,
                      label,
                      None
                    )
                  )
                )
                json <- io2f(d.toJson)
                ok   <- Ok(json)
              } yield ok
          }
        } yield response
      }

  }
  private val relativeBase = Defaults.relativeBase
  private val logger       = getLogger

  private def parseInt(s: String): Either[String, Int] =
    Try(s.toInt).map(Right(_)).getOrElse(Left(s"$s is not a number"))

  private def errJson(msg: String): IO[Response[IO]] =
    Ok(Json.fromFields(List(("error", Json.fromString(msg)))))

}

object DataService {
  def apply(client: Client[IO]): DataService =
    new DataService(client)
}
