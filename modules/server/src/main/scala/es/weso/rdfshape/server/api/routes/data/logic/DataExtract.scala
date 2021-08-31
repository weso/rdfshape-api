package es.weso.rdfshape.server.api.routes.data.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.nodes.{IRI, Lang}
import es.weso.rdfshape.server.api.definitions.ApiDefaults.{
  defaultSchemaEngine,
  defaultSchemaFormat,
  defaultShapeLabel
}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.utils.json.JsonUtils._
import es.weso.schema.Schema
import es.weso.schemaInfer.{InferOptions, PossiblePrefixes, SchemaInfer}
import es.weso.shapemaps.{NodeSelector, ResultShapeMap}
import es.weso.utils.IOUtils.{ESIO, either2es, io2es, run_es}
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
final case class DataExtract private (
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
          ("message", Json.fromString(msg)),
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

/** Static utilities to extract schemas from RDF data
  */
object DataExtract extends LazyLogging {

  /** Extract Shex from a given RDF input
    *
    * @param rdf             Input RDF
    * @param optData         Input data (optional)
    * @param optDataFormat   Input data format (optional)
    * @param optNodeSelector Node selector (optional)
    * @param optInference    Conversion inference (optional)
    * @param optEngine       Conversion engine (optional)
    * @param optSchemaFormat Target schema format (optional)
    * @param optLabelName    Label name (optional)
    * @param relativeBase    Relative base
    * @return
    */
  def dataExtract(
      rdf: RDFReasoner,
      optData: Option[String],
      optDataFormat: Option[DataFormat],
      optNodeSelector: Option[String],
      optInference: Option[String],
      optEngine: Option[String],
      optSchemaFormat: Option[SchemaFormat],
      optLabelName: Option[String],
      relativeBase: Option[IRI]
  ): IO[DataExtract] = {
    val base         = relativeBase.map(_.str)
    val engine       = optEngine.getOrElse(defaultSchemaEngine)
    val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat)
    optNodeSelector match {
      case None =>
        IO.pure(
          DataExtract.fromMsg("DataExtract: Node selector not specified")
        )
      case Some(nodeSelector) =>
        val es: ESIO[(Schema, ResultShapeMap)] = for {
          pm       <- io2es(rdf.getPrefixMap)
          selector <- either2es(NodeSelector.fromString(nodeSelector, base, pm))
          eitherResult <- {
            logger.debug(s"Node selector: $selector")

            val inferOptions: InferOptions = InferOptions(
              inferTypePlainNode = true,
              addLabelLang = Some(Lang("en")),
              possiblePrefixMap = PossiblePrefixes.wikidataPrefixMap,
              maxFollowOn = 1,
              followOnLs = List(),
              followOnThreshold = Some(1),
              sortFunction = InferOptions.orderByIRI
            )
            io2es(
              SchemaInfer.runInferSchema(
                rdf,
                selector,
                engine,
                optLabelName.map(IRI(_)).getOrElse(defaultShapeLabel),
                inferOptions
              )
            )
          }
          pair <- either2es(eitherResult)
          str  <- io2es(pair._1.serialize("ShExC"))
          _    <- io2es(IO(logger.debug(s"Extracted; $str")))
        } yield {
          pair
        }
        for {
          either <- run_es(es)
        } yield either.fold(
          err => DataExtract.fromMsg(err),
          pair => {
            val (schema, resultShapeMap) = pair
            DataExtract.fromExtraction(
              optData,
              optDataFormat,
              schemaFormat.name,
              engine,
              schema,
              resultShapeMap
            )
          }
        )
    }
  }

  /** @param msg Error message contained in the result
    * @return A DataExtractResult consisting of a single error message and no data
    */
  def fromMsg(msg: String): DataExtract =
    DataExtract(msg, None, None, None, None, None, None)

  /** @return A DataExtractResult, given all the parameters needed to build it (input, formats and results)
    */
  def fromExtraction(
      optData: Option[String],
      optDataFormat: Option[DataFormat],
      schemaFormat: String,
      schemaEngine: String,
      schema: Schema,
      resultShapeMap: ResultShapeMap
  ): DataExtract =
    DataExtract(
      "Shape extracted",
      optData,
      optDataFormat,
      optSchemaFormat = Some(schemaFormat),
      optSchemaEngine = Some(schemaEngine),
      optSchema = Some(schema),
      optResultShapeMap = Some(resultShapeMap)
    )
}
