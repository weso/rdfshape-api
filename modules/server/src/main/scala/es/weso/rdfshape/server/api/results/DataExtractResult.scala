package es.weso.rdfshape.server.api.results

import cats.effect.IO
import es.weso.rdfshape.server.api.Defaults.{defaultSchemaEngine, defaultSchemaFormat}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.utils.json.JsonUtilsServer._
import es.weso.schema.Schema
import es.weso.shapemaps.ResultShapeMap
import io.circe.Json

case class DataExtractResult private (
    msg: String,
    optData: Option[String],
    optDataFormat: Option[DataFormat],
    optSchemaFormat: Option[String],
    optSchemaEngine: Option[String],
    optSchema: Option[Schema],
    optResultShapeMap: Option[ResultShapeMap]
) {
  def toJson: IO[Json] = optSchema match {
    case None => IO(Json.fromFields(List(("msg", Json.fromString(msg)))))
    case Some(schema) => {
      val engine       = optSchemaEngine.getOrElse(defaultSchemaEngine)
      val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat.name)
      for {
        schemaStr <- schema.serialize(schemaFormat)
      } yield Json.fromFields(
        List(
          ("msg", Json.fromString(msg)),
          ("inferedShape", Json.fromString(schemaStr)),
          ("schemaFormat", Json.fromString(schemaFormat)),
          ("schemaEngine", Json.fromString(engine))
        ) ++
          maybeField(optData, "data", Json.fromString) ++
          maybeField(
            optDataFormat,
            "dataFormat",
            (df: DataFormat) => Json.fromString(df.name)
          ) ++
          maybeField(
            optResultShapeMap,
            "resultShapeMap",
            (r: ResultShapeMap) => Json.fromString(r.toString)
          )
      )
    }
  }
}

object DataExtractResult {
  def fromMsg(msg: String): DataExtractResult =
    DataExtractResult(msg, None, None, None, None, None, None)
  def fromExtraction(
      optData: Option[String],
      optDataFormat: Option[DataFormat],
      schemaFormat: String,
      schemaEngine: String,
      schema: Schema,
      resultShapeMap: ResultShapeMap
  ): DataExtractResult =
    DataExtractResult(
      "Shape extracted",
      optData,
      optDataFormat,
      optSchemaFormat = Some(schemaFormat),
      optSchemaEngine = Some(schemaEngine),
      optSchema = Some(schema),
      optResultShapeMap = Some(resultShapeMap)
    )
}
