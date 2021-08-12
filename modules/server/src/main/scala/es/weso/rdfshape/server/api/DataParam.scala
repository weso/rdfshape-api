package es.weso.rdfshape.server.api

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, RDFReasoner}
import es.weso.rdfshape.server.api.Defaults._
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.merged.CompoundData
import es.weso.rdfshape.server.html2rdf.HTML2RDF
import es.weso.utils.IOUtils._

import java.net.URI

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
) extends LazyLogging {

  val dataFormat: Option[DataFormat] = {
    val dataTab = parseDataTab(activeDataTab.getOrElse(defaultActiveDataTab))
    logger.debug(s"Data tab received: $dataTab")
    dataTab match {
      case Right(`dataUrlType`)  => dataFormatUrl orElse dataFormatValue
      case Right(`dataFileType`) => dataFormatFile orElse dataFormatValue
      case Right(`dataTextAreaType`) =>
        dataFormatTextarea orElse dataFormatValue
      case _ => dataFormatValue
    }
  }

  /** get RDF data from data parameters
    *
    * @return a pair where the first value can be Some(string)
    *         if it has string representation and the second parameter
    *         is the resource with the RDF data
    */
  def getData(
      relativeBase: Option[IRI]
  ): IO[(Option[String], Resource[IO, RDFReasoner])] = {
    val base = relativeBase.map(_.str)
    logger.debug(s"ActiveDataTab: $activeDataTab")
    val inputType = activeDataTab match {
      case Some(a)                         => parseDataTab(a)
      case None if compoundData.isDefined  => Right(compoundDataType)
      case None if data.isDefined          => Right(dataTextAreaType)
      case None if dataURL.isDefined       => Right(dataUrlType)
      case None if dataFile.isDefined      => Right(dataFileType)
      case None if maybeEndpoint.isDefined => Right(dataEndpointType)
      case None                            => Right(dataTextAreaType)
    }
    logger.debug(s"Input type: $inputType")
    val x: IO[(Option[String], Resource[IO, RDFReasoner])] = inputType match {

      case Right(`compoundDataType`) =>
        logger.debug(s"Input - compoundDataType: $data")
        for {
          cd <- IO.fromEither(
            CompoundData
              .fromString(compoundData.getOrElse(""))
              .leftMap(s => new RuntimeException(s))
          )
          res <- cd.toRDF
        } yield (None, res)

      case Right(`dataUrlType`) =>
        logger.debug(s"Input - dataUrlType: $data")
        dataURL match {
          case None => err(s"Non value for dataURL")
          case Some(dataUrl) =>
            val dataFormat = dataFormatUrl.getOrElse(DataFormat.default)
            for {
              rdf <- rdfFromUri(new URI(dataUrl), dataFormat, base)
            } yield (None, rdf)
        }
      case Right(`dataFileType`) =>
        logger.debug(s"Input - dataFileType: $data")
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
        logger.debug(s"Input - dataEndpointType: $data")
        maybeEndpoint match {
          case None => err(s"No value for endpoint")
          case Some(endpointUrl) =>
            for {
              endpoint <- Endpoint.fromString(endpointUrl)
              // newRdf <- extendWithInference(endpoint, inference)
            } yield (None, Resource.pure[IO, RDFReasoner](endpoint))
        }
      case Right(`dataTextAreaType`) =>
        logger.debug(s"Input - dataTextAreaType: $data")
        data match {
          case None => RDFAsJenaModel.empty.flatMap(e => IO((None, e)))
          case d @ Some(data) =>
            val dataFormat = dataFormatTextarea.getOrElse(
              dataFormatValue.getOrElse(DataFormat.default)
            )
            val x: IO[(Option[String], Resource[IO, RDFReasoner])] = for {
              res <- rdfFromString(data, dataFormat, base)
              res2 = extendWithInference(
                res.onFinalize(showFinalize),
                inference
              )
            } yield (d, res2)
            x
        }

      case Right(other) => {
        val msg = s"Unknown value for activeDataTab: $other"
        logger.error(msg)
        err(msg)
      }

      case Left(msg) => {
        logger.error(msg)
        err(msg)
      }
    }
    x
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

  private def showFinalize: IO[Unit] = IO {
    logger.debug("Closing RDF data")
  }

  private def rdfFromString(
      str: String,
      format: Format,
      base: Option[String]
  ): IO[Resource[IO, RDFReasoner]] = {
    logger.debug(s"RDF from string with format: $format")
    format.name match {
      case formatName if HTML2RDF.availableExtractorNames contains formatName =>
        IO(
          HTML2RDF.extractFromString(str, formatName)
        ) /*for {
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
      case formatName if HTML2RDF.availableExtractorNames contains formatName =>
        IO(
          HTML2RDF.extractFromUrl(
            uri.toString,
            formatName
          )
        )
      case _ =>
        for {
          baseIri <- mkBase(base)
          res     <- RDFAsJenaModel.fromURI(uri.toString, format.name, baseIri)
        } yield res
    }
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
    logger.debug(s"Applying inference $optInference")
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

  private def mkBaseIri(
      maybeBase: Option[String]
  ): Either[String, Option[IRI]] = maybeBase match {
    case None      => Right(None)
    case Some(str) => IRI.fromString(str).map(Some(_))
  }

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

}

object DataParam extends LazyLogging {

  private[api] def mkData(
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

  private[api] def mkDataParam(partsMap: PartsMap): IO[DataParam] = for {
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

    logger.debug(s"data: $data")
    logger.debug(s"compoundData: $compoundData")
    logger.debug(s"dataFormatValue: $dataFormatValue")
    logger.debug(s"dataFormatTextArea: $dataFormatTextArea")
    logger.debug(s"dataFormatUrl: $dataFormatUrl")
    logger.debug(s"dataFormatFile: $dataFormatFile")
    logger.debug(s"dataURL: $dataURL")
    logger.debug(s"endpoint: $endpoint")
    logger.debug(s"activeDataTab: $activeDataTab")
    logger.debug(s"targetDataFormat: $targetDataFormat")
    logger.debug(s"inference: $inference")

    val endpointRegex = "Endpoint: (.+)".r
    val finalEndpoint = endpoint.fold(data match {
      case None => None
      case Some(str) =>
        str match {
          case endpointRegex(ep) => Some(ep)
          case _                 => None
        }
    })(Some(_))
    val finalActiveDataTab = activeDataTab
    logger.debug(s"Final endpoint: $finalEndpoint")

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
            logger.warn(s"Unsupported dataFormat for $name: $str")
            None
          },
          df => Some(df)
        )
  }

  private[api] def empty: DataParam =
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
