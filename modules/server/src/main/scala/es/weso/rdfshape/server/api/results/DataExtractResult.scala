package es.weso.rdfshape.server.api.results

import cats.effect.IO
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.routes.Defaults.{
  defaultSchemaEngine,
  defaultSchemaFormat
}
import es.weso.rdfshape.server.utils.json.JsonUtils._
import es.weso.schema.Schema
import es.weso.shapemaps.ResultShapeMap
import io.circe.Json

/** Data class representing the output of an extraction operation (input RDF data => output schema)
  *
  * @param msg               Output informational message after conversion. Used in case of error.
  * @param optData           RDF input data from which ShEx may be extracted
  * @param optDataFormat     RDF input data format
  * @param optSchemaFormat   Target schema format
  * @param optSchemaEngine   Target schema engine
  * @param optSchema         Resulting schema
  * @param optResultShapeMap Resulting shapemap
  */
case class DataExtractResult private (
    msg: String,
    optData: Option[String],
    optDataFormat: Option[DataFormat],
    optSchemaFormat: Option[String],
    optSchemaEngine: Option[String],
    optSchema: Option[Schema],
    optResultShapeMap: Option[ResultShapeMap]
) {

  /** Convert an extraction result to its JSON representation
    *
    * @return JSON representation of the extraction result
    */
  def toJson: IO[Json] = optSchema match {
    case None => IO(Json.fromFields(List(("msg", Json.fromString(msg)))))
    case Some(schema) =>
      val engine       = optSchemaEngine.getOrElse(defaultSchemaEngine)
      val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat.name)
      for {
        schemaStr <- schema.serialize(schemaFormat)
      } yield Json.fromFields(
        List(
          ("msg", Json.fromString(msg)),
          ("inferredShape", Json.fromString(schemaStr)),
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

object DataExtractResult {

  /** @param msg Error message contained in the result
    * @return A DataExtractResult consisting of a single error message and no data
    */
  def fromMsg(msg: String): DataExtractResult =
    DataExtractResult(msg, None, None, None, None, None, None)

  /** @return A DataExtractResult, given all the parameters needed to build it (input, formats and results)
    */
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
