package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect._
import cats.implicits.toBifunctorOps
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

import scala.util.Try

/** Data class representing a single RDF data instance with its inner content, format and source
  *
  * @note Invalid initial data is accepted, but may cause errors when operating with it.
  * @param content   RDF data, as it is received before being processed depending on the [[source]]
  * @param format    Data format
  * @param inference Data inference
  * @param source    Origin source, used to know how to process the raw data
  */
sealed case class DataSingle(
    private val content: String,
    override val format: DataFormat = DataFormat.default,
    inference: InferenceEngine = NONE,
    override val source: DataSource = DataSource.default
) extends Data
    with LazyLogging {

  // Non empty content
  assume(!content.isBlank, "Could not build the RDF from empty data")
  // Valid source
  assume(
    DataSource.values.exists(_ equalsIgnoreCase source),
    s"Unknown data source: '$source'"
  )

  override lazy val fetchedContents: Either[String, String] = {
    if(source equalsIgnoreCase DataSource.URL)
      getUrlContents(content)
    // Text or file
    else Right(content)
  }

  // Fetched contents successfully
  assume(
    fetchedContents.isRight,
    fetchedContents.left.getOrElse("Unknown error creating the data")
  )

  override val raw: String = fetchedContents.toOption.get

  /** Given an RDF source of data, try to get the RDF model representation
    *
    * @return RDF (Jena) model of RDF data received from a client
    */
  override def toRdf(
      relativeBase: Option[IRI] = None
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    for {
      rdf <- rdfFromString(raw, format, relativeBase.map(_.str))
      result = rdf.evalMap(rdf => rdf.applyInference(inference))
    } yield result
  }

  /** @param input RDF data as a raw string
    * @param format RDF data format
    * @param base   Base
    * @return An RDF model extracted from the input data
    */
  private def rdfFromString(
      input: String,
      format: DataFormat,
      base: Option[String]
  ): IO[Resource[IO, RDFAsJenaModel]] = {
    logger.debug(s"RDF from string with format: $format")
    val formatName = format.name
    if(HtmlToRdf.availableExtractorNames.contains(formatName))
      IO(HtmlToRdf.extractFromString(input, formatName))
    else
      for {
        baseIri <- mkBase(base)
        res     <- RDFAsJenaModel.fromChars(input, format.name, baseIri)
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

  override implicit val encode: Encoder[DataSingle] =
    (data: DataSingle) =>
      Json.obj(
        ("content", data.fetchedContents.toOption.asJson),
        ("format", data.format.asJson),
        ("inference", data.inference.asJson),
        ("source", data.source.asJson)
      )
  override implicit val decode: Decoder[Either[String, DataSingle]] =
    (cursor: HCursor) => {
      val dataInfo = for {
        content <- cursor
          .downField(ContentParameter.name)
          .as[String]
          .map(_.trim)

        format <- cursor
          .downField(FormatParameter.name)
          .as[Either[String, RdfFormat]]

        inference <-
          cursor
            .downField(InferenceParameter.name)
            .as[InferenceEngine]

        source <- cursor
          .downField(SourceParameter.name)
          .as[DataSource]

      } yield (content, format, inference, source)

      dataInfo.map {
        /* Destructure and try to build the object, catch the exception as error
         * message if needed */
        case (content, maybeFormat, inference, source) =>
          for {
            format <- maybeFormat
            data <- Try {
              DataSingle(content, format, inference, source)
            }.toEither.leftMap(err =>
              s"Could not build the schema from user data:\n ${err.getMessage}"
            )
          } yield data
      }
    }

  override def mkData(partsMap: PartsMap): IO[Either[String, DataSingle]] =
    for {
      // Data param as sent by client
      paramData <- partsMap
        .optPartValue(DataParameter.name)
        .map(_.getOrElse(""))
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
        content = paramData,
        format = format,
        inference = inference,
        source = dataSource
      )

    } yield data.fetchedContents.map(_ => data)

  /** @param inferenceStr String representing the inference value
    * @return Optionally, the inference contained in a given data string
    */
  private def getInference(
      inferenceStr: Option[String]
  ): Option[InferenceEngine] = {
    inferenceStr.flatMap(InferenceEngine.fromString(_).toOption)
  }
}
