package es.weso.rdfshape.server.api.routes.data.logic.data

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, NONE, RDFReasoner}
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.data.DataSource.DataSource
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
  * @param optEndpoint      TODO: remove eventually. Optionally, a data endpoint
  * @param dataFormat       Data format
  * @param inference        Data inference
  * @param targetDataFormat Data target format (only for conversion operations)
  * @param activeDataSource Active source, used to know which source the data comes from
  */
sealed case class SimpleData(
    dataRaw: String,
    optEndpoint: Option[String],
    dataFormat: DataFormat,
    inference: InferenceEngine,
    targetDataFormat: Option[DataFormat],
    activeDataSource: DataSource
) extends Data
    with LazyLogging {

  override val dataSource: DataSource = activeDataSource

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

  //  def toRdfJena: IO[Resource[IO, RDFAsJenaModel]] = {
  //    for {
  //      rdf <- RDFAsJenaModel.fromString(
  //        dataRaw,
  //        dataFormat.name,
  //        None,
  /* useBNodeLabels = if(activeDataSource != DataSource.URL) false else true */
  //      )
  //    } yield rdf
  //  }

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

}

private[api] object SimpleData
    extends DataCompanion[SimpleData]
    with LazyLogging {

  /** Empty data representation, with no inner data and all defaults to none
    */
  override lazy val emptyData: SimpleData =
    SimpleData(
      dataRaw = emptyDataValue,
      optEndpoint = None,
      dataFormat = DataFormat.defaultFormat,
      inference = NONE,
      targetDataFormat = None,
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

  override implicit val encodeData: Encoder[SimpleData] = (data: SimpleData) =>
    {
      Json.obj(
        ("data", Json.fromString(data.dataRaw)),
        ("source", Json.fromString(data.activeDataSource)),
        ("format", data.dataFormat.asJson),
        ("targetFormat", data.targetDataFormat.asJson),
        ("inference", data.inference.asJson)
      )

    }
  override implicit val decodeData: Decoder[SimpleData] =
    (cursor: HCursor) => {
      for {
        data <- cursor.downField("data").as[String]

        dataFormat <- cursor
          .downField("format")
          .as[DataFormat]
          .orElse(Right(ApiDefaults.defaultDataFormat))

        targetDataFormat <- cursor
          .downField("targetFormat")
          .as[Option[DataFormat]]

        dataInference <-
          cursor
            .downField("inference")
            .as[Option[InferenceEngine]]

        dataSource <- cursor
          .downField("source")
          .as[DataSource]
          .orElse(Right(DataSource.defaultActiveDataSource))

        base = SimpleData.emptyData.copy(
          dataRaw = data,
          dataFormat = dataFormat,
          targetDataFormat = targetDataFormat,
          activeDataSource = dataSource,
          inference = dataInference.getOrElse(NONE)
        )

      } yield base
    }

  override def mkData(partsMap: PartsMap): IO[Either[String, SimpleData]] =
    for {
      dataStr  <- partsMap.optPartValue(DataParameter.name)
      dataUrl  <- partsMap.optPartValue(DataUrlParameter.name)
      dataFile <- partsMap.optPartValue(DataFileParameter.name)
      paramFormat <- DataFormat.fromRequestParams(
        DataFormatParameter.name,
        partsMap
      )
      paramInference <- partsMap.optPartValue(InferenceParameter.name)
      targetDataFormat <- DataFormat.fromRequestParams(
        TargetDataFormatParameter.name,
        partsMap
      )
      paramDataSource <- partsMap.optPartValue(ActiveDataSourceParameter.name)

      // Confirm final format and inference
      inference = getInference(paramInference).getOrElse(NONE)
      format    = paramFormat.getOrElse(ApiDefaults.defaultDataFormat)

      // Check the client's selected source
      dataSource = paramDataSource.getOrElse(DataSource.defaultActiveDataSource)
      _          = logger.debug(s"RDF Data received - Source: $dataSource")

      // Base for the result
      base = SimpleData.emptyData.copy(
        dataFormat = format,
        inference = inference,
        targetDataFormat = targetDataFormat
      )

      // Create the data
      maybeData: Either[String, SimpleData] = dataSource match {
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

  /** @param inferenceStr String representing the inference value
    * @return Optionally, the inference contained in a given data string
    */
  private def getInference(
      inferenceStr: Option[String]
  ): Option[InferenceEngine] = {
    inferenceStr.flatMap(InferenceEngine.fromString(_).toOption)
  }

}
