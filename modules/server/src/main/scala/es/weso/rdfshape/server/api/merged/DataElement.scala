package es.weso.rdfshape.server.api.merged
import cats.effect._
import cats.implicits._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.Defaults
import es.weso.rdfshape.server.api.format.DataFormat
import io.circe._

case class DataElement(
    data: Option[String],
    dataUrl: Option[String],
    endpoint: Option[String],
    dataFile: Option[String],
    dataFormat: DataFormat,
    activeDataTab: ActiveDataTab
) {
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
      pprint.log("ERROR DATA ELEMENT")
      IO.raiseError(
        new RuntimeException(
          s"Not implemented yet compound with activeTab: ${activeDataTab}"
        )
      )
  }
}

object DataElement {

  val empty: DataElement = DataElement(
    None,
    None,
    None,
    None,
    Defaults.defaultDataFormat,
    ActiveDataTab.default
  )

  implicit val encodeDataElement: Encoder[DataElement] =
    new Encoder[DataElement] {
      final def apply(a: DataElement): Json = a.activeDataTab match {
        case DataTextArea =>
          Json.obj(
            ("data", Json.fromString(a.data.getOrElse(""))),
            ("activeTab", Json.fromString(a.activeDataTab.id)),
            ("dataFormat", Json.fromString(a.dataFormat.name))
          )
        case DataUrl =>
          Json.obj(
            ("dataURL", Json.fromString(a.dataUrl.getOrElse(""))),
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
    }

  implicit val decodeDataElement: Decoder[DataElement] =
    new Decoder[DataElement] {
      final def apply(c: HCursor): Decoder.Result[DataElement] = {
        for {
          dataActiveTab <- parseActiveTab(c)
          dataFormat    <- parseDataFormat(c)
          base = DataElement.empty.copy(
            dataFormat = dataFormat,
            activeDataTab = dataActiveTab
          )
          rest <- dataActiveTab match {
            case DataTextArea =>
              for {
                data <- c.downField("data").as[String]
              } yield base.copy(data = Some(data))
            case DataFile =>
              /* TODO: either send the file text through the request (bad idea)
               * or decode the file appropriately */
              pprint.log(c.downField("dataFile"))
              for {
                dataFile <- c.downField("dataFile").as[String]
              } yield base.copy(dataFile = Some(dataFile))
            case DataUrl =>
              for {
                dataUrl <- c.downField("dataURL").as[String]
              } yield base.copy(dataUrl = Some(dataUrl))
            case DataEndpoint =>
              for {
                endpoint <- c.downField("endpoint").as[String]
              } yield base.copy(endpoint = Some(endpoint))
          }
        } yield rest
      }

      private def parseActiveTab(c: HCursor): Decoder.Result[ActiveDataTab] = {
        for {
          str <- c.downField("activeTab").as[String] orElse Right(
            ActiveDataTab.default.id
          )
          a <- ActiveDataTab.fromString(str).leftMap(DecodingFailure(_, List()))
        } yield a
      }

      private def parseDataFormat(c: HCursor): Decoder.Result[DataFormat] =
        for {
          str <- c
            .downField("dataFormat")
            .as[String]
            .orElse(Right(Defaults.defaultDataFormat.name))
          df <- DataFormat
            .fromString(str)
            .leftMap(s =>
              DecodingFailure(s"Non supported dataFormat: $s", List())
            )
        } yield df

    }

}
