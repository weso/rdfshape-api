package es.weso.rdfshape.server.api.merged

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import io.circe._

/** Represent each chunk of RDF data submitted (mainly on RDF-merging operations)
  *
  * @param data          Raw RDF data (plain text)
  * @param dataUrl       URL containing the RDF data
  * @param endpoint      RDF data endpoint to use
  * @param dataFile      File containing the RDF data
  * @param dataFormat    Format of the RDF data
  * @param activeDataTab Active tab in the client's view, used to choose which RDF source should be read
  */
case class DataElement(
    data: Option[String],
    dataUrl: Option[String],
    dataFile: Option[String],
    endpoint: Option[String],
    dataFormat: DataFormat,
    activeDataTab: ActiveDataTab
) extends LazyLogging {

  /** Given an RDF source of data sent by a client, try to parse it and get the RDF model representation
    *
    * @return RDF (Jena) model of RDF data received from a client
    * @note Iteratively compares the different possible values of activeDataTab against the one the client attached to decide an extracting strategy
    */
  def toRDF: IO[Resource[IO, RDFAsJenaModel]] = activeDataTab match {

    case DataTextArea =>
      for {
        rdf <- RDFAsJenaModel.fromString(
          data.getOrElse(""),
          dataFormat.name,
          None,
          useBNodeLabels = false
        )
      } yield rdf

    case DataUrl =>
      for {
        rdf <- RDFAsJenaModel.fromURI(
          dataUrl.getOrElse(""),
          dataFormat.name,
          None
        )
      } yield rdf

    case _ =>
      logger.error(s"Data element error")
      IO.raiseError(
        new RuntimeException(
          s"Not implemented yet compound with activeTab: $activeDataTab"
        )
      )
  }
}

object DataElement extends LazyLogging {

  /** Empty and most basic data element
    */
  val empty: DataElement = DataElement(
    data = None,
    dataUrl = None,
    dataFile = None,
    endpoint = None,
    ApiDefaults.defaultDataFormat,
    ActiveDataTab.default
  )

  /** Encoder used to transform DataElement instances to JSON values
    */
  implicit val encodeDataElement: Encoder[DataElement] =
    (a: DataElement) =>
      a.activeDataTab match {
        case DataTextArea =>
          Json.obj(
            ("data", Json.fromString(a.data.getOrElse(""))),
            ("activeTab", Json.fromString(a.activeDataTab.id)),
            ("dataFormat", Json.fromString(a.dataFormat.name))
          )
        case DataUrl =>
          Json.obj(
            ("dataUrl", Json.fromString(a.dataUrl.getOrElse(""))),
            ("activeTab", Json.fromString(a.activeDataTab.id)),
            ("dataFormat", Json.fromString(a.dataFormat.name))
          )
        case DataFile =>
          Json.obj(
            ("dataFile", Json.fromString(a.dataFile.getOrElse(""))),
            ("activeTab", Json.fromString(a.activeDataTab.id)),
            ("dataFormat", Json.fromString(a.dataFormat.name))
          )
        case DataEndpoint =>
          Json.obj(
            ("endpoint", Json.fromString(a.endpoint.getOrElse(""))),
            ("activeTab", Json.fromString(a.activeDataTab.id)),
            ("dataFormat", Json.fromString(a.dataFormat.name))
          )
      }

  /** Decoder used to extract DataElement instances from JSON values
    */
  implicit val decodeDataElement: Decoder[DataElement] =
    new Decoder[DataElement] {
      final def apply(cursor: HCursor): Decoder.Result[DataElement] = {
        for {
          dataActiveTab <- parseActiveTab(cursor)
          dataFormat    <- parseDataFormat(cursor)
          base = DataElement.empty.copy(
            dataFormat = dataFormat,
            activeDataTab = dataActiveTab
          )
          rest <- dataActiveTab match {
            case DataTextArea =>
              logger.debug("Data element decoder - DataTextArea")
              for {
                data <- cursor.downField("data").as[String]
              } yield base.copy(data = Some(data))
            case DataFile =>
              logger.debug("Data element decoder - DataFile")
              /* TODO: either send the file text through the request (bad idea)
               * or decode the file appropriately */
              logger.debug(cursor.downField("dataFile").toString)
              for {
                dataFile <- cursor.downField("dataFile").as[String]
              } yield base.copy(dataFile = Some(dataFile))
            case DataUrl =>
              logger.debug("Data element decoder - DataUrl")
              for {
                dataUrl <- cursor.downField("dataUrl").as[String]
              } yield base.copy(dataUrl = Some(dataUrl))
            case DataEndpoint =>
              logger.debug("Data element decoder - DataEndpoint")
              for {
                endpoint <- cursor.downField("endpoint").as[String]
              } yield base.copy(endpoint = Some(endpoint))
          }
        } yield rest
      }

      /** @param cursor Cursor to operate JSON abstractions
        * @return The ActiveDataTab specified in a JSON encoded DataElement
        */
      private def parseActiveTab(
          cursor: HCursor
      ): Decoder.Result[ActiveDataTab] = {
        for {
          activeTabId <- cursor.downField("activeTab").as[String] orElse Right(
            ActiveDataTab.default.id
          )
          a <- ActiveDataTab
            .fromString(activeTabId)
            .leftMap(DecodingFailure(_, List()))
        } yield a
      }

      /** @param cursor Cursor to operate JSON abstractions
        * @return The DataFormat specified in a JSON encoded DataElement
        */
      private def parseDataFormat(cursor: HCursor): Decoder.Result[DataFormat] =
        for {
          dataFormatStr <- cursor
            .downField("dataFormat")
            .as[String]
            .orElse(Right(ApiDefaults.defaultDataFormat.name))
          dataFormat <- DataFormat
            .fromString(dataFormatStr)
            .leftMap(s =>
              DecodingFailure(s"Non supported dataFormat: $s", List())
            )
        } yield dataFormat
    }
}
