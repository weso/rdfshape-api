package es.weso.rdfshape.server.api.routes.data.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.{IRI, Lang}
import es.weso.rdf.{PrefixMap, RDFReasoner}
import es.weso.rdfshape.server.api.definitions.ApiDefaults.{
  defaultSchemaEngine,
  defaultSchemaFormat,
  defaultShapeLabel
}
import es.weso.rdfshape.server.api.definitions.UmlDefinitions.umlOptions
import es.weso.rdfshape.server.api.format.{DataFormat, SchemaFormat}
import es.weso.rdfshape.server.utils.json.JsonUtils.maybeField
import es.weso.schema.{DataFormats, Schema}
import es.weso.schemaInfer.{InferOptions, PossiblePrefixes, SchemaInfer}
import es.weso.shapemaps.{NodeSelector, ResultShapeMap}
import es.weso.uml.{Schema2UML, UML}
import es.weso.utils.IOUtils._
import io.circe.Json

/** Static utilities used by the {@link es.weso.rdfshape.server.api.routes.data.service.DataService}
  * to operate on RDF data
  */
private[api] object DataOperations extends LazyLogging {

  /** @param df Data format
    * @return The given data format or the default one in case none was provided
    */
  def dataFormatOrDefault(df: Option[String]): String =
    df.getOrElse(DataFormats.defaultFormatName)

  /** For a given RDF input (plain text), return information about it
    *
    * @param data          Input data string
    * @param dataFormatStr Input data format
    * @return Information about the input RDF: statements, well-formed, etc.
    */
  def dataInfoFromString(
      data: String,
      dataFormatStr: String
  ): IO[DataInfoResult] = {
    val either: ESIO[DataInfoResult] = for {
      dataFormat <- either2es(DataFormat.fromString(dataFormatStr))
      json <- io2es(
        RDFAsJenaModel
          .fromChars(data, dataFormat.name)
          .flatMap(_.use(rdf => dataInfo(rdf, Some(data), Some(dataFormat))))
      )
    } yield json

    either.fold(e => DataInfoResult.fromMsg(e), identity)
  }

  /** For a given RDF input, return information about it
    *
    * @param rdf        Input RDF
    * @param data       Input data string
    * @param dataFormat Input data format
    * @return Information about the input RDF: statements, well-formed, etc.
    */
  def dataInfo(
      rdf: RDFReasoner,
      data: Option[String],
      dataFormat: Option[DataFormat]
  ): IO[DataInfoResult] = {
    val either: IO[Either[Throwable, DataInfoResult]] = (for {
      numberStatements <- rdf.getNumberOfStatements()
      predicates       <- rdf.predicates().compile.toList
      pm               <- rdf.getPrefixMap
    } yield DataInfoResult.fromData(
      data,
      dataFormat,
      predicates.toSet,
      numberStatements,
      pm
    )).attempt
    either.map(
      _.fold(e => DataInfoResult.fromMsg(e.getMessage), r => r)
    )
  }

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
  ): IO[DataExtractResult] = {
    val base         = relativeBase.map(_.str)
    val engine       = optEngine.getOrElse(defaultSchemaEngine)
    val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat)
    optNodeSelector match {
      case None =>
        IO.pure(
          DataExtractResult.fromMsg("DataExtract: Node selector not specified")
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
          err => DataExtractResult.fromMsg(err),
          pair => {
            val (schema, resultShapeMap) = pair
            DataExtractResult.fromExtraction(
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

  // TODO: remove if unused?
  def shapeInfer(
      rdf: RDFReasoner,
      optNodeSelector: Option[String],
      optInference: Option[String],
      optEngine: Option[String],
      optSchemaFormat: Option[SchemaFormat],
      optLabelName: Option[String],
      relativeBase: Option[IRI],
      withUml: Boolean
  ): ESIO[Json] = {
    val base         = relativeBase.map(_.str)
    val engine       = optEngine.getOrElse(defaultSchemaEngine)
    val schemaFormat = optSchemaFormat.getOrElse(defaultSchemaFormat)
    optNodeSelector match {
      case None => ok_es(Json.Null)
      case Some(nodeSelector) =>
        for {
          pm       <- io2es(rdf.getPrefixMap)
          selector <- either2es(NodeSelector.fromString(nodeSelector, base, pm))
          eitherResult <- io2es {
            logger.debug(s"Selector: $selector")

            SchemaInfer.runInferSchema(
              rdf,
              selector,
              engine,
              optLabelName.map(IRI(_)).getOrElse(defaultShapeLabel)
            )
          }
          result <- either2es(eitherResult)
          (schemaInfer, resultMap) = result
          maybePair <-
            if(withUml)
              either2es(Schema2UML.schema2UML(schemaInfer).map(Some(_)))
            else ok_es(None)
          maybeSvg <- io2es(maybePair match {
            case None => IO.pure(None)
            case Some(pair) =>
              val (uml, warnings) = pair
              uml.toSVG(umlOptions).map(Some(_))
          })
          str <- io2es(schemaInfer.serialize(schemaFormat.name))
        } yield Json.fromFields(
          List(
            ("inferredShape", Json.fromString(str)),
            ("format", Json.fromString(schemaFormat.name)),
            ("engine", Json.fromString(engine)),
            ("nodeSelector", Json.fromString(nodeSelector))
          ) ++
            maybeField(
              maybePair,
              "uml",
              (pair: (UML, List[String])) => {
                val (uml, warnings) = pair
                Json.fromString(uml.toPlantUML(umlOptions))
              }
            ) ++
            maybeField(maybeSvg, "svg", Json.fromString)
        )
    }
  }

  /** Convert a given prefix map to JSON format for API operations
    *
    * @param prefixMap Input prefix map
    * @return JSON representation of the prefix map
    */
  private[api] def prefixMap2Json(prefixMap: PrefixMap): Json = {
    Json.fromFields(prefixMap.pm.map { case (prefix, iri) =>
      (prefix.str, Json.fromString(iri.getLexicalForm))
    })
  }

}
