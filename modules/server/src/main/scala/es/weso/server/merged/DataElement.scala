package es.weso.server.merged
import cats._
import cats.data._
import cats.implicits._
import io.circe._
import es.weso.server.helper.DataFormat
import es.weso.server.Defaults
import es.weso.server.helper.DataFormat
import es.weso.utils.IOUtils._
import es.weso.rdf.jena.RDFAsJenaModel
import cats.effect.IO
import es.weso.rdf.RDFReader
import es.weso.rdf.RDFReasoner

case class DataElement(
    data: Option[String],
    dataUrl: Option[String],
    endpoint: Option[String],
    dataFile: Option[String],
    dataFormat: DataFormat,
    activeDataTab: ActiveDataTab
) {
 def toRDF: EitherT[IO,String,RDFAsJenaModel] = activeDataTab match {
      case DataTextArea => for {
        rdf <- io2es(RDFAsJenaModel.fromString(data.getOrElse(""), dataFormat.name,None,false))
      } yield rdf
      case _ => fail_es(s"Not implemented yet compound with activeDataTab: ${activeDataTab}")
  }  
}

object DataElement {

  val empty: DataElement = DataElement(None, None, None, None, Defaults.defaultDataFormat, ActiveDataTab.default)

  implicit val encodeDataElement: Encoder[DataElement] = new Encoder[DataElement] {
    final def apply(a: DataElement): Json = a.activeDataTab match {
      case DataTextArea =>
        Json.obj(
          ("data", Json.fromString(a.data.getOrElse(""))),
          ("activeDataTab", Json.fromString(a.activeDataTab.id)),
          ("dataFormat", Json.fromString(a.dataFormat.name))
        )
      case DataUrl =>
        Json.obj(
          ("dataUrl", Json.fromString(a.dataUrl.getOrElse(""))),
          ("activeDataTab", Json.fromString(a.activeDataTab.id)),
          ("dataFormat", Json.fromString(a.dataFormat.name))
        )
      case DataFile =>
        Json.obj(
          ("dataFile", Json.fromString(a.dataFile.getOrElse(""))),
          ("activeDataTab", Json.fromString(a.activeDataTab.id)),
          ("dataFormat", Json.fromString(a.dataFormat.name))
        )
      case DataEndpoint =>
        Json.obj(
          ("endpoint", Json.fromString(a.endpoint.getOrElse(""))),
          ("activeDataTab", Json.fromString(a.activeDataTab.id)),
          ("dataFormat", Json.fromString(a.dataFormat.name))
        )
    }
  }

  implicit val decodeDataElement: Decoder[DataElement] = new Decoder[DataElement] {
    final def apply(c: HCursor): Decoder.Result[DataElement] = {
      for {
        dataActiveTab <- parseActiveTab(c)
        dataFormat    <- parseDataFormat(c)
        base = DataElement.empty.copy(dataFormat = dataFormat)
        rest <- dataActiveTab match {
          case DataTextArea =>
            for {
              data <- c.downField("data").as[String]
            } yield base.copy(data = Some(data))
          case DataFile =>
            for {
              dataFile <- c.downField("dataFile").as[String]
            } yield base.copy(dataFile = Some(dataFile))
          case DataUrl =>
            for {
              dataUrl <- c.downField("dataUrl").as[String]
            } yield base.copy(dataUrl = Some(dataUrl))
          case DataEndpoint =>
            for {
              endpoint <- c.downField("endpoint").as[String]
            } yield base.copy(endpoint = Some(endpoint))
        }
      } yield rest
    }

    private def parseActiveTab(c: HCursor): Decoder.Result[ActiveDataTab] =
      for {
        str <- c.downField("activeDataTab").as[String] orElse Right(ActiveDataTab.default.id)
        a   <- ActiveDataTab.fromString(str).leftMap(DecodingFailure(_, List()))
      } yield a

    private def parseDataFormat(c: HCursor): Decoder.Result[DataFormat] = 
      for {
         str <- c.downField("dataFormat").as[String].orElse(Right(Defaults.defaultDataFormat.name))
         df <- DataFormat.fromString(str).leftMap(s => DecodingFailure(s"Non supported dataFormat: $s", List()))
      } yield df

  }

}
