package es.weso.server

import java.net.URI

import cats.effect._
import cats.implicits._
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.server.Defaults._
import es.weso.server.format._
import es.weso.server.merged.CompoundData
import es.weso.utils.IOUtils._
import org.log4s.getLogger
import es.weso.rdf.InferenceEngine

case class DataParam(
    data: Option[String],
    dataURL: Option[String],
    dataFile: Option[String],
    maybeEndpoint: Option[String],
    dataFormatValue: Option[DataFormat],
    dataFormatTextarea: Option[DataFormat],
    dataFormatUrl: Option[DataFormat],
    dataFormatFile: Option[DataFormat],
    inference: Option[String],
    targetDataFormat: Option[DataFormat],
    activeDataTab: Option[String],
    compoundData: Option[String]
) {
  private[this] val logger = getLogger

  sealed abstract class DataInputType {
    val id: String
  }
  case object dataUrlType extends DataInputType {
    override val id = "#dataUrl"
  }
  case object dataFileType extends DataInputType {
    override val id = "#dataFile"
  }
  case object dataEndpointType extends DataInputType {
    override val id = "#dataEndpoint"
  }
  case object dataTextAreaType extends DataInputType {
    override val id = "#dataTextArea"
  }
  case object compoundDataType extends DataInputType {
    override val id = "#compoundData"
  }

  def parseDataTab(tab: String): Either[String, DataInputType] = {
    logger.debug(s"parseDataTab: tab = $tab")
    val inputTypes =
      List(dataUrlType, dataFileType, dataEndpointType, dataTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None =>
        Left(
          s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]"
        )
    }
  }

  val dataFormat: Option[DataFormat] = {
    val dataTab = parseDataTab(activeDataTab.getOrElse(defaultActiveDataTab))
    pprint.log(dataTab)
    dataTab match {
      case Right(`dataUrlType`)  => dataFormatUrl orElse dataFormatValue
      case Right(`dataFileType`) => dataFormatFile orElse dataFormatValue
      case Right(`dataTextAreaType`) =>
        dataFormatTextarea orElse dataFormatValue
      case _ => dataFormatValue
    }
  }

  private def applyInference(
      rdf: Resource[IO, RDFReasoner],
      inference: Option[String],
      dataFormat: Format
  ): Resource[IO, RDFReasoner] =
    extendWithInference(rdf, inference)

  private def extendWithInference(
      resourceRdf: Resource[IO, RDFReasoner],
      optInference: Option[String]
  ): Resource[IO, RDFReasoner] = {
    logger.debug(s"############# Applying inference $optInference")
    optInference match {
      case None => resourceRdf
      case Some(str) =>
        InferenceEngine.fromString(str) match {
          case Right(engine) =>
            resourceRdf.evalMap(rdf => rdf.applyInference(engine))
          case Left(err) =>
            // TODO: Check how to invoke using Resource.raiseError...
            throw new RuntimeException(
              s"Error parsing inference engine param ($str): $err"
            )
        }

    }
  }

  /** get RDF data from data parameters
    * @return a pair where the first value can be Some(string)
    *         if it has string representation and the second parameter
    *         is the resource with the RDF data
    */
  def getData(
      relativeBase: Option[IRI]
  ): IO[(Option[String], Resource[IO, RDFReasoner])] = {
    val base = relativeBase.map(_.str)
    pprint.log(s"ActiveDataTab: $activeDataTab")
    val inputType = activeDataTab match {
      case Some(a)                         => parseDataTab(a)
      case None if compoundData.isDefined  => Right(compoundDataType)
      case None if data.isDefined          => Right(dataTextAreaType)
      case None if dataURL.isDefined       => Right(dataUrlType)
      case None if dataFile.isDefined      => Right(dataFileType)
      case None if maybeEndpoint.isDefined => Right(dataEndpointType)
      case None                            => Right(dataTextAreaType)
    }
    pprint.log(inputType)
    val x: IO[(Option[String], Resource[IO, RDFReasoner])] = inputType match {

      case Right(`compoundDataType`) =>
        for {
          cd <- IO.fromEither(
            CompoundData
              .fromString(compoundData.getOrElse(""))
              .leftMap(s => new RuntimeException(s))
          )
          res <- cd.toRDF
        } yield (None, res)

      case Right(`dataUrlType`) =>
        dataURL match {
          case None => err(s"Non value for dataURL")
          case Some(dataUrl) =>
            val dataFormat = dataFormatUrl.getOrElse(DataFormat.default)
            for {
              rdf <- rdfFromUri(new URI(dataUrl), dataFormat, base)
            } yield (None, rdf)
        }
      case Right(`dataFileType`) =>
        dataFile match {
          case None => err(s"No value for dataFile")
          case Some(dataStr) =>
            val dataFormat: Format =
              dataFormatFile.getOrElse(DataFormat.default)
            /* io2es(RDFAsJenaModel.fromString(dataStr, dataFormat.name,
             * iriBase).use(rdf => for { iriBase <- mkBase(base) newRdf <-
             * extendWithInference(rdf, inference) eitherStr <-
             * newRdf.serialize(dataFormat.name,None).attempt optStr =
             * eitherStr.toOption } yield (optStr,newRdf))) */
            for {
              iriBase <- mkBase(base)
              res <- RDFAsJenaModel.fromString(
                dataStr,
                dataFormat.name,
                iriBase
              )
              res2 = extendWithInference(res, inference)
            } yield (None, res2)
        }

      case Right(`dataEndpointType`) =>
        maybeEndpoint match {
          case None => err(s"No value for endpoint")
          case Some(endpointUrl) =>
            for {
              endpoint <- Endpoint.fromString(endpointUrl)
              // newRdf <- extendWithInference(endpoint, inference)
            } yield (None, Resource.pure[IO, RDFReasoner](endpoint))
        }
      case Right(`dataTextAreaType`) =>
        pprint.log(data)
        data match {
          case None => RDFAsJenaModel.empty.flatMap(e => IO((None, e)))
          case d @ Some(data) =>
            val dataFormat = dataFormatTextarea.getOrElse(
              dataFormatValue.getOrElse(DataFormat.default)
            )
            val x: IO[(Option[String], Resource[IO, RDFReasoner])] = for {
              _   <- IO { pprint.log("@@@ DataTextArea") }
              res <- rdfFromString(data, dataFormat, base)
              res2 = extendWithInference(
                res.onFinalize(showFinalize),
                inference
              )
            } yield (d, res2)
            x
        }

      case Right(other) => err(s"Unknown value for activeDataTab: $other")

      case Left(msg) => err(msg)
    }
    x
  }

  private def showFinalize: IO[Unit] = IO { println(s"Closing RDF data") }

  private def rdfFromString(
      str: String,
      format: Format,
      base: Option[String]
  ): IO[Resource[IO, RDFReasoner]] = {
    pprint.log(format)
    format.name match {
      case f if HTML2RDF.availableExtractorNames contains f =>
        IO(HTML2RDF.extractFromString(str, f)) /*for {
        eitherRdf <-
      } yield eitherRdf */
      case _ =>
        for {
          baseIri <- mkBase(base)
          res     <- RDFAsJenaModel.fromChars(str, format.name, baseIri)
        } yield res
    }
  }

  private def rdfFromUri(
      uri: URI,
      format: Format,
      base: Option[String]
  ): IO[Resource[IO, RDFReasoner]] = {
    format.name.toLowerCase match {
      case f if HTML2RDF.availableExtractorNames contains f =>
        IO(HTML2RDF.extractFromUrl(uri.toString, f))
      case _ =>
        for {
          baseIri <- mkBase(base)
          res     <- RDFAsJenaModel.fromURI(uri.toString, format.name, baseIri)
        } yield res
    }
  }

  private def mkBaseIri(
      maybeBase: Option[String]
  ): Either[String, Option[IRI]] = maybeBase match {
    case None      => Right(None)
    case Some(str) => IRI.fromString(str).map(Some(_))
  }

  private def mkBase(base: Option[String]): IO[Option[IRI]] = base match {
    case None => IO(None)
    case Some(str) =>
      IRI
        .fromString(str)
        .fold(
          e => IO.raiseError(new RuntimeException(s"Cannot get IRI from $str")),
          (iri: IRI) => IO(Some(iri))
        )
  }

}

object DataParam {

  private[this] val logger = getLogger

  private[server] def mkData(
      partsMap: PartsMap,
      relativeBase: Option[IRI]
  ): IO[(Resource[IO, RDFReasoner], DataParam)] = {

    val r: IO[(Resource[IO, RDFReasoner], DataParam)] = for {
      dp   <- mkDataParam(partsMap)
      pair <- dp.getData(relativeBase)
    } yield {
      val (optStr, rdf) = pair
      (rdf, dp.copy(data = optStr))
    }
    r
  }

  private def getDataFormat(
      name: String,
      partsMap: PartsMap
  ): IO[Option[DataFormat]] = for {
    maybeStr <- partsMap.optPartValue(name)
  } yield maybeStr match {
    case None => None
    case Some(str) =>
      DataFormat
        .fromString(str)
        .fold(
          err => {
            pprint.log(s"Unsupported dataFormat for ${name}: $str")
            None
          },
          df => Some(df)
        )
  }

  private[server] def mkDataParam(partsMap: PartsMap): IO[DataParam] = for {
    data               <- partsMap.optPartValue("data")
    compoundData       <- partsMap.optPartValue("compoundData")
    dataURL            <- partsMap.optPartValue("dataURL")
    dataFile           <- partsMap.optPartValue("dataFile")
    endpoint           <- partsMap.optPartValue("endpoint")
    dataFormatTextArea <- getDataFormat("dataFormatTextArea", partsMap)
    dataFormatUrl      <- getDataFormat("dataFormatUrl", partsMap)
    dataFormatFile     <- getDataFormat("dataFormatFile", partsMap)
    dataFormatValue    <- getDataFormat("dataFormat", partsMap)
    inference          <- partsMap.optPartValue("inference")
    targetDataFormat   <- getDataFormat("targetDataFormat", partsMap)
    activeDataTab      <- partsMap.optPartValue("activeTab")
  } yield {
    pprint.log(data)
    pprint.log(compoundData)
    pprint.log(dataFormatValue)
    pprint.log(dataFormatTextArea)
    pprint.log(dataFormatUrl)
    pprint.log(dataFormatFile)
    pprint.log(dataURL)
    pprint.log(endpoint)
    pprint.log(activeDataTab)
    pprint.log(targetDataFormat)
    pprint.log(inference)
    val endpointRegex = "Endpoint: (.+)".r
    val finalEndpoint = endpoint.fold(data match {
      case None => None
      case Some(str) =>
        str match {
          case endpointRegex(ep) => Some(ep)
          case _                 => None
        }
    })(Some(_))
    val finalActiveDataTab = activeDataTab /* finalEndpoint match {
      case Some(endpoint) =>
        if (endpoint.length > 0) Some("#dataEndpoint")
        else activeDataTab
      case None => activeDataTab
    } */
    pprint.log(finalEndpoint)

    val dp = DataParam(
      data,
      dataURL,
      dataFile,
      finalEndpoint,
      dataFormatValue,
      dataFormatTextArea,
      dataFormatUrl,
      dataFormatFile,
      inference,
      targetDataFormat,
      finalActiveDataTab,
      compoundData
    )
    dp
  }

  private[server] def empty: DataParam =
    DataParam(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )

}
