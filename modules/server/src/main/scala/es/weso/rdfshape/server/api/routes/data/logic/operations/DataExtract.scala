package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.nodes.{IRI, Lang}
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.definitions.ApiDefaults._
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataExtract.{
  DataExtractResult,
  successMessage
}
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.schema.Schema
import es.weso.schemaInfer.{InferOptions, PossiblePrefixes, SchemaInfer}
import es.weso.shapemaps.{NodeSelector, ResultShapeMap}
import es.weso.utils.IOUtils.{either2es, io2es}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a schema-extraction operation (input RDF data => output schema)
  *
  * @param inputData          RDF input data from which ShEx may be extracted
  * @param schemaFormat Target schema format
  * @param schemaEngine Target schema engine
  * @param result    Object of type [[DataExtractResult]] containing the results of the data extraction
  */
final case class DataExtract private (
    override val inputData: Data,
    schemaFormat: SchemaFormat = ApiDefaults.defaultSchemaFormat,
    schemaEngine: Schema = ApiDefaults.defaultSchemaEngine,
    result: DataExtractResult
) extends DataOperation(successMessage, inputData)

/** Static utilities to extract schemas from RDF data
  */
private[api] object DataExtract extends LazyLogging {

  private val successMessage = "Extraction successful"

  /** Common infer options to all extraction operations
    */
  private val inferOptions: InferOptions = InferOptions(
    inferTypePlainNode = true,
    addLabelLang = Some(Lang("en")),
    possiblePrefixMap = PossiblePrefixes.wikidataPrefixMap,
    maxFollowOn = 1,
    followOnLs = List(),
    followOnThreshold = Some(1),
    sortFunction = InferOptions.orderByIRI
  )

  /** Extract Shex from a given RDF input
    *
    * @param inputData          Input data for the extraction
    * @param nodeSelector       Node selector for the schema extraction
    * @param optTargetSchemaEngine Optionally, the target conversion engine. Defaults to [[ApiDefaults.defaultSchemaEngine]].
    * @param optTargetSchemaFormat Optionally, the target schema format. Defaults to [[ApiDefaults.defaultSchemaFormat]].
    * @param optLabel       Label IRI (optional). Defaults to [[ApiDefaults.defaultShapeLabel]]
    * @param relativeBase       Relative base
    * @return
    */
  def dataExtract(
      inputData: Data,
      nodeSelector: String,
      optTargetSchemaEngine: Option[Schema],
      optTargetSchemaFormat: Option[SchemaFormat],
      optLabel: Option[IRI],
      relativeBase: Option[IRI]
  ): IO[DataExtract] = {

    val base = relativeBase.map(_.str)
    val targetSchemaEngine =
      optTargetSchemaEngine.getOrElse(ApiDefaults.defaultSchemaEngine)
    val targetSchemaFormat =
      optTargetSchemaFormat.getOrElse(ApiDefaults.defaultSchemaFormat)

    for {
      rdf <- inputData.toRdf() // Get rdf resource
      eitherResult <- rdf.use(rdfReader => {
        val results: EitherT[IO, String, (Schema, ResultShapeMap)] = for {
          pm <- io2es(rdfReader.getPrefixMap)
          ns <- either2es(
            NodeSelector.fromString(nodeSelector, base, pm)
          )

          resultPair <-
            EitherT(
              SchemaInfer.runInferSchema(
                rdfReader,
                ns,
                targetSchemaEngine.name,
                optLabel.getOrElse(defaultShapeLabel),
                inferOptions
              )
            )

          _ <- io2es(IO(logger.debug(s"Extracted schema")))
        } yield resultPair

        results.value
      })

      finalResult <- eitherResult.fold(
        err => IO.raiseError(new RuntimeException(err)),
        pair => {
          val (resultSchema, resultShapemap) = pair
          IO {
            DataExtract(
              inputData = inputData,
              schemaFormat = targetSchemaFormat,
              schemaEngine = targetSchemaEngine,
              result = DataExtractResult(
                targetSchemaFormat = targetSchemaFormat,
                schema = resultSchema,
                shapeMap = resultShapemap
              )
            )
          }
        }
      )
    } yield finalResult
  }

  /** Encoder for [[DataExtractResult]]
    */
  private implicit val encodeDataExtractResult: Encoder[DataExtractResult] =
    (dataExtract: DataExtractResult) =>
      Json.fromFields(
        List(
          (
            "schema",
            Json.fromString(
              dataExtract.schema
                .serialize(dataExtract.targetSchemaFormat.name)
                .unsafeRunSync()
            )
          ),
          ("shapeMap", Json.fromString(dataExtract.shapeMap.toString))
        )
      )

  /** Convert a [[DataExtract]] to its JSON representation
    *
    * @return JSON representation of the extraction result
    */

  implicit val encodeDataExtractOperation: Encoder[DataExtract] =
    (dataExtract: DataExtract) => {
      Json.fromFields(
        List(
          ("message", Json.fromString(dataExtract.successMessage)),
          ("data", dataExtract.inputData.asJson),
          ("schemaFormat", dataExtract.schemaFormat.asJson),
          ("schemaEngine", Json.fromString(dataExtract.schemaEngine.name)),
          ("result", dataExtract.result.asJson)
        )
      )
    }

  /** Case class representing the results to be returned when performing a data-info operation
    */
  final case class DataExtractResult private (
      targetSchemaFormat: SchemaFormat,
      schema: Schema,
      shapeMap: ResultShapeMap
  )
}
