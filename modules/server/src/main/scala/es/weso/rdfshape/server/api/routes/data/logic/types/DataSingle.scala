package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, NONE, RDFReasoner}
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.html2rdf.HTML2RDF
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.utils.IOUtils.err
import io.circe._
import io.circe.syntax.EncoderOps

/** Data class representing a single RDF data instance with its current format and source
  *
  * @note Invalid initial data is accepted, but may cause errors when operating with it.
  * @param dataRaw          RDF data raw text
  * @param dataFormat       Data format
  * @param inference        Data inference
  * @param activeDataSource Active source, used to know which source the data comes from
  */
sealed case class DataSingle(
    dataRaw: String,
    dataFormat: DataFormat,
    inference: InferenceEngine,
    activeDataSource: DataSource
) extends Data
    with LazyLogging {

  override lazy val rawData: Option[String] = Some(dataRaw)
  override val dataSource: DataSource       = activeDataSource
  override val format: Option[DataFormat]   = Some(dataFormat)

  /** Given an RDF source of data, try to get the RDF model representation
    *
    * @return RDF (Jena) model of RDF data received from a client
    * @note Iteratively compares the different possible values of activeDataTab against the one the client attached to decide an extracting strategy
    */
  override def toRdf(
      relativeBase: Option[IRI] = None
  ): IO[Resource[IO, RDFAsJenaModel]] = {

    if(dataRaw.isBlank)
      RDFAsJenaModel.empty.flatMap(e => IO(e))
    else
      for {
        rdf <- rdfFromString(dataRaw, dataFormat, relativeBase.map(_.str))
        result = rdf.evalMap(rdf => rdf.applyInference(inference))
      } yield result
  }

  /** Get RDF data from data parameters
    *
    * @return The resource capable of reading the RDF data
    */
  def getRdfResource(
      relativeBase: Option[IRI]
  ): IO[Resource[IO, RDFReasoner]] = {
    val base = relativeBase.map(_.str)

    val x: IO[Resource[IO, RDFReasoner]] =
      activeDataSource match {

        case DataSource.TEXT | DataSource.URL | DataSource.FILE =>
          logger.debug(s"Input - $activeDataSource: $dataRaw")
          if(dataRaw.isBlank)
            RDFAsJenaModel.empty.flatMap(e => IO(e))
          else
            for {
              rdf <- rdfFromString(dataRaw, dataFormat, base)
              result = rdf.evalMap(rdf => rdf.applyInference(inference))
            } yield result

        case other =>
          val msg = s"Unknown value for data source: $other"
          logger.error(msg)
          err(msg)
      }
    x
  }

  /** @param data  RDF data as a raw string
    * @param format RDF data format
    * @param base   Base
    * @return An RDF model extracted from the input data
    */
  private def rdfFromString(
      data: String,
      format: DataFormat,
      base: Option[String]
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    logger.debug(s"RDF from string with format: $format")
    val formatName = format.name
    if(HTML2RDF.availableExtractorNames contains formatName)
      IO(HTML2RDF.extractFromString(data, formatName))
    else
      for {
        baseIri <- mkBase(base)
        res     <- RDFAsJenaModel.fromChars(data, format.name, baseIri)
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

  override def toString: String = dataRaw
}

private[api] object DataSingle
    extends DataCompanion[DataSingle]
    with LazyLogging {

  /** Empty data representation, with no inner data and all defaults to none
    */
  override lazy val emptyData: DataSingle =
    DataSingle(
      dataRaw = emptyDataValue,
      dataFormat = DataFormat.defaultFormat,
      inference = NONE,
      activeDataSource = DataSource.defaultActiveDataSource
    )

  /** Placeholder value used for the raw data whenever an empty data is issued/needed.
    */
  val emptyDataValue = ""

  /** Auxiliar encoder for data inference
    */
  private implicit val encodeInference: Encoder[InferenceEngine] =
    (inference: InferenceEngine) => {
      Json.obj(("name", Json.fromString(inference.name)))
    }

  /** Auxiliar decoder for data inference
    */
  private implicit val decodeInference: Decoder[InferenceEngine] =
    (cursor: HCursor) =>
      for {
        inferenceName <- cursor.downField("name").as[String]
        inference = InferenceEngine
          .fromString(inferenceName)
          .toOption
          .getOrElse(NONE)
      } yield inference

  override implicit val encodeData: Encoder[DataSingle] =
    (data: DataSingle) =>
      Json.obj(
        ("data", Json.fromString(data.dataRaw)),
        ("source", Json.fromString(data.activeDataSource)),
        ("format", data.dataFormat.asJson),
        ("inference", data.inference.asJson)
      )

  override def mkData(partsMap: PartsMap): IO[Either[String, DataSingle]] =
    for {
      dataStr  <- partsMap.optPartValue(DataParameter.name)
      dataUrl  <- partsMap.optPartValue(DataUrlParameter.name)
      dataFile <- partsMap.optPartValue(DataFileParameter.name)
      paramFormat <- DataFormat.fromRequestParams(
        DataFormatParameter.name,
        partsMap
      )
      paramInference <- partsMap.optPartValue(InferenceParameter.name)

      paramDataSource <- partsMap.optPartValue(ActiveDataSourceParameter.name)

      // Confirm final format and inference
      inference = getInference(paramInference).getOrElse(NONE)
      format    = paramFormat.getOrElse(ApiDefaults.defaultDataFormat)

      // Check the client's selected source
      dataSource = paramDataSource.getOrElse(DataSource.defaultActiveDataSource)
      _          = logger.debug(s"RDF Data received - Source: $dataSource")

      // Base for the result
      base = DataSingle.emptyData.copy(
        dataFormat = format,
        inference = inference
      )

      // Create the data
      maybeData: Either[String, DataSingle] = dataSource match {
        case DataSource.TEXT =>
          dataStr match {
            case None => Left("No value for the data string")
            case Some(dataRaw) =>
              Right(
                base.copy(
                  dataRaw = dataRaw.trim,
                  activeDataSource = DataSource.TEXT
                )
              )
          }
        case DataSource.URL =>
          dataUrl match {
            case None => Left("No value for the data url")
            case Some(url) =>
              getUrlContents(url) match {
                case Right(dataRaw) =>
                  Right(
                    base.copy(
                      dataRaw = dataRaw.trim,
                      activeDataSource = DataSource.URL
                    )
                  )
                case Left(err) => Left(s"Could not read data: $err")
              }
          }
        case DataSource.FILE =>
          dataFile match {
            case None => Left("No value for the data file")
            case Some(dataRaw) =>
              Right(
                base.copy(
                  dataRaw = dataRaw.trim,
                  activeDataSource = DataSource.FILE
                )
              )
          }
        case other =>
          val msg = s"Unknown data source: $other"
          logger.warn(msg)
          Left(msg)
      }
    } yield maybeData

  override implicit val decodeData: Decoder[DataSingle] =
    (cursor: HCursor) => {
      for {
        data <- cursor.downField("data").as[String]

        dataFormat <- cursor
          .downField("format")
          .as[DataFormat]
          .orElse(Right(ApiDefaults.defaultDataFormat))

        dataInference <-
          cursor
            .downField("inference")
            .as[Option[InferenceEngine]]

        dataSource <- cursor
          .downField("source")
          .as[DataSource]
          .orElse(Right(DataSource.defaultActiveDataSource))

        base = DataSingle.emptyData.copy(
          dataRaw = data,
          dataFormat = dataFormat,
          activeDataSource = dataSource,
          inference = dataInference.getOrElse(NONE)
        )

      } yield base
    }

  /** @param inferenceStr String representing the inference value
    * @return Optionally, the inference contained in a given data string
    */
  private def getInference(
      inferenceStr: Option[String]
  ): Option[InferenceEngine] = {
    inferenceStr.flatMap(InferenceEngine.fromString(_).toOption)
  }
}
