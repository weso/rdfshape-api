package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, NONE}
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.{DataFormat, RdfFormat}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.aux.InferenceCodecs._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.html2rdf.HtmlToRdf
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import io.circe._
import io.circe.syntax.EncoderOps

/** Data class representing a single RDF data instance with its inner content, format and source
  *
  * @note Invalid initial data is accepted, but may cause errors when operating with it.
  * @param dataPre    RDF data, as it is received before being processed depending on the [[dataSource]]
  * @param dataFormat Data format
  * @param inference  Data inference
  * @param dataSource Origin source, used to know how to process the raw data
  */
sealed case class DataSingle(
    private val dataPre: Option[String],
    private val dataFormat: DataFormat,
    inference: InferenceEngine,
    override val dataSource: DataSource
) extends Data
    with LazyLogging {

  /** Given the user input [[dataPre]] for the data and its source, fetch the Data contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Either an error creating the raw data or a String containing the final text
    */
  override lazy val rawData: Either[String, String] =
    dataPre.map(_.trim) match {
      case None | Some("") => Left("Could not build the RDF from empty data")
      case Some(userData) =>
        dataSource match {
          case DataSource.TEXT | // Raw text input by user
              DataSource.FILE |  // File input already decoded to string
              DataSource.COMPOUND => // Compound data already processed by server
            Right(userData)

          case DataSource.URL =>
            getUrlContents(userData)

          case other =>
            val msg = s"Unknown data source: $other"
            logger.warn(msg)
            Left(msg)
        }
    }

  // Override and make publicly available the trait properties
  override val format: Option[DataFormat] = Option(dataFormat)

  /** Given an RDF source of data, try to get the RDF model representation
    *
    * @return RDF (Jena) model of RDF data received from a client
    * @note Iteratively compares the different possible values of activeDataTab against the one the client attached to decide an extracting strategy
    */
  override def toRdf(
      relativeBase: Option[IRI] = None
  ): IO[Resource[IO, RDFAsJenaModel]] = {

    rawData match {
      case Right(data) =>
        for {
          rdf <- rdfFromString(data, dataFormat, relativeBase.map(_.str))
          result = rdf.evalMap(rdf => rdf.applyInference(inference))

        } yield result
      case Left(_) => RDFAsJenaModel.empty.flatMap(e => IO(e))
    }

  }

  override def toString: String = rawData.toString

  /** @param dataStr RDF data as a raw string
    * @param format   RDF data format
    * @param base     Base
    * @return An RDF model extracted from the input data
    */
  private def rdfFromString(
      dataStr: String,
      format: DataFormat,
      base: Option[String]
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    logger.debug(s"RDF from string with format: $format")
    val formatName = format.name
    if(HtmlToRdf.availableExtractorNames.contains(formatName))
      IO(HtmlToRdf.extractFromString(dataStr, formatName))
    else
      for {
        baseIri <- mkBase(base)
        res     <- RDFAsJenaModel.fromChars(dataStr, format.name, baseIri)
      } yield res

  }

  /** @param base RDF base
    * @return For a given base, an IRI representing it
    */
  private def mkBase(base: Option[String]): IO[Option[IRI]] = base match {
    case None => IO(None)
    case Some(str) =>
      IRI
        .fromString(str)
        .fold(
          _ =>
            IO.raiseError(new RuntimeException(s"Could not get IRI from $str")),
          (iri: IRI) => IO(Some(iri))
        )
  }
}

private[api] object DataSingle
    extends DataCompanion[DataSingle]
    with LazyLogging {

  /** Empty data representation, with no inner data and all defaults or None
    */
  override lazy val emptyData: DataSingle =
    DataSingle(
      dataPre = None,
      dataFormat = ApiDefaults.defaultDataFormat,
      inference = NONE,
      dataSource = ApiDefaults.defaultDataSource
    )

  override implicit val encodeData: Encoder[DataSingle] =
    (data: DataSingle) =>
      Json.obj(
        ("data", data.rawData.toOption.asJson),
        ("format", data.dataFormat.asJson),
        ("inference", data.inference.asJson),
        ("source", data.dataSource.asJson)
      )
  override implicit val decodeData: Decoder[DataSingle] =
    (cursor: HCursor) =>
      for {
        data <- cursor.downField("data").as[Option[String]]

        dataFormat <- cursor
          .downField("dataFormat")
          .as[RdfFormat]

        dataInference <-
          cursor
            .downField("dataInference")
            .as[InferenceEngine]

        dataSource <- cursor
          .downField("dataSource")
          .as[DataSource]
          .orElse(Right(DataSource.default))

        decoded = DataSingle.emptyData.copy(
          dataPre = data,
          dataFormat = dataFormat,
          dataSource = dataSource,
          inference = dataInference
        )

      } yield decoded

  override def mkData(partsMap: PartsMap): IO[Either[String, DataSingle]] =
    for {
      // Data param as sent by client
      paramData <- partsMap.optPartValue(DataParameter.name)
      paramFormat <- DataFormat.fromRequestParams(
        DataFormatParameter.name,
        partsMap
      )

      paramInference <- partsMap.optPartValue(InferenceParameter.name)

      paramDataSource <- partsMap.optPartValue(DataSourceParameter.name)

      // Confirm final format and inference
      inference = getInference(paramInference).getOrElse(NONE)
      format    = paramFormat.getOrElse(ApiDefaults.defaultDataFormat)

      // Check the client's selected source
      dataSource = paramDataSource.getOrElse(DataSource.default)
      _          = logger.debug(s"RDF Data received - Source: $dataSource")

      // Create the data instance
      data = DataSingle(
        dataPre = paramData,
        dataFormat = format,
        inference = inference,
        dataSource = dataSource
      )

    } yield data.rawData.map(_ => data)

  /** @param inferenceStr String representing the inference value
    * @return Optionally, the inference contained in a given data string
    */
  private def getInference(
      inferenceStr: Option[String]
  ): Option[InferenceEngine] = {
    inferenceStr.flatMap(InferenceEngine.fromString(_).toOption)
  }
}
