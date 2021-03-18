package es.weso.server.results

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.schema.Schema
import es.weso.server.ApiHelper
import es.weso.server.Defaults.{defaultSchemaEngine, defaultSchemaFormat}
import es.weso.server.format._
import es.weso.shapemaps.ResultShapeMap
import es.weso.uml.UML
import es.weso.utils.json.JsonUtilsServer._
import io.circe.Json
import cats.effect.IO

case class DataExtractResult private(msg: String,
                                     optData: Option[String],
                                     optDataFormat: Option[DataFormat],
                                     optSchemaFormat: Option[String],
                                     optSchemaEngine: Option[String],
                                     optSchema: Option[Schema],
                                     optResultShapeMap: Option[ResultShapeMap],
                                    ) {
  def toJson: IO[Json] = optSchema match {
    case None => IO(Json.fromFields(List(("msg", Json.fromString(msg)))))
    case Some(schema) => {
      val engine = optSchemaEngine.getOrElse(defaultSchemaEngine)
      val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat.name)
      for {
        schemaStr <- schema.serialize(schemaFormat)
      } yield Json.fromFields(List(
         ("msg", Json.fromString(msg)),
         ("inferedShape", Json.fromString(schemaStr)),
         ("schemaFormat", Json.fromString(schemaFormat)),
         ("schemaEngine", Json.fromString(engine))
        ) ++
        maybeField(optData, "data", Json.fromString(_)) ++
        maybeField(optDataFormat, "dataFormat", (df: DataFormat) => Json.fromString(df.name)) ++
        maybeField(optResultShapeMap, "resultShapeMap", (r:ResultShapeMap) => Json.fromString(r.toString))
      )
    }
  }
}

object DataExtractResult {
  def fromMsg(msg: String): DataExtractResult = DataExtractResult(msg, None, None, None, None, None, None)
  def fromExtraction(optData: Option[String],
                     optDataFormat: Option[DataFormat],
                     schemaFormat: String,
                     schemaEngine: String,
                     schema: Schema,
                     resultShapeMap: ResultShapeMap): DataExtractResult =
    DataExtractResult("Shape extracted",
      optData,
      optDataFormat,
      optSchemaFormat = Some(schemaFormat),
      optSchemaEngine = Some(schemaEngine),
      optSchema = Some(schema),
      optResultShapeMap = Some(resultShapeMap)
      )
}
