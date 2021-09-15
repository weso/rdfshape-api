package es.weso.rdfshape.server.api.routes.data.logic

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, RDFReasoner}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.merged.CompoundData
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.html2rdf.HTML2RDF
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.utils.IOUtils.err

import java.net.URI
import scala.util.matching.Regex

/** Data class representing RDF data and its current source
  *
  * @param data
  * @param dataUrl
  * @param dataFile
  * @param optEndpoint
  * @param optDataFormat    Data format
  * @param inference        Data inference
  * @param targetDataFormat Data target format (only for conversion operations)
  * @param activeDataTab    Active tab, used to know which source the data comes from
  * @param compoundData
  */
sealed case class Data(
    data: Option[String],
    dataUrl: Option[String],
    dataFile: Option[String],
    optEndpoint: Option[String],
    optDataFormat: Option[DataFormat],
    inference: Option[String],
    targetDataFormat: Option[DataFormat],
    activeDataTab: Option[String],
    compoundData: Option[String]
) extends LazyLogging {
  val dataFormat: DataFormat = optDataFormat.getOrElse(
    DataFormat.defaultFormat
  )

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
      case Some(a)                        => parseDataTab(a)
      case None if compoundData.isDefined => Right(compoundDataType)
      case None if data.isDefined         => Right(dataTextAreaType)
      case None if dataUrl.isDefined      => Right(dataUrlType)
      case None if dataFile.isDefined     => Right(dataFileType)
      case None if optEndpoint.isDefined  => Right(dataEndpointType)
      case None                           => Right(dataTextAreaType)
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
        dataUrl match {
          case None => err(s"Non value for dataUrl")
          case Some(dataUrl) =>
            for {
              rdf <- rdfFromUri(
                new URI(dataUrl),
                dataFormat,
                base
              )
            } yield (None, rdf)
        }
      case Right(`dataFileType`) =>
        logger.debug(s"Input - dataFileType: $data")
        dataFile match {
          case None => err(s"No value for dataFile")
          case Some(dataStr) =>
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
        optEndpoint match {
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
            val x: IO[(Option[String], Resource[IO, RDFReasoner])] = for {
              res <- rdfFromString(data, dataFormat, base)
              res2 = extendWithInference(
                res.onFinalize(showFinalize),
                inference
              )
            } yield (d, res2)
            x
        }

      case Right(other) =>
        val msg = s"Unknown value for activeDataTab: $other"
        logger.error(msg)
        err(msg)

      case Left(msg) =>
        logger.error(msg)
        err(msg)
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

  private def rdfFromUri(
      uri: URI,
      format: Format,
      base: Option[String]
  ): IO[Resource[IO, RDFReasoner]] = {

    getUrlContents(uri.toString) match {
      case Left(errMsg) => IO.raiseError(new RuntimeException(errMsg))
      case _ =>
        format.name.toLowerCase match {
          case formatName
              if HTML2RDF.availableExtractorNames contains formatName =>
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

private[api] object Data extends LazyLogging {

  /** Regular expressions used for identifying if a custom endpoint was given for this data sample
    */
  private val endpointRegex: Regex = "Endpoint: (.+)".r

  def mkData(
      partsMap: PartsMap,
      relativeBase: Option[IRI]
  ): IO[(Resource[IO, RDFReasoner], Data)] = {

    val r: IO[(Resource[IO, RDFReasoner], Data)] = for {
      data <- mkData(partsMap)
      pair <- data.getData(relativeBase)
    } yield {
      val (optStr, rdf) = pair
      (rdf, data.copy(data = optStr))
    }
    r
  }

  def mkData(partsMap: PartsMap): IO[Data] = for {
    data         <- partsMap.optPartValue(DataParameter.name)
    dataUrl      <- partsMap.optPartValue(DataUrlParameter.name)
    dataFile     <- partsMap.optPartValue(DataFileParameter.name)
    compoundData <- partsMap.optPartValue(CompoundDataParameter.name)
    endpoint     <- partsMap.optPartValue(EndpointParameter.name)
    dataFormat <- DataFormat.fromRequestParams(
      DataFormatParameter.name,
      partsMap
    )
    inference <- partsMap.optPartValue(InferenceParameter.name)
    targetDataFormat <- DataFormat.fromRequestParams(
      TargetDataFormatParameter.name,
      partsMap
    )
    activeDataTab <- partsMap.optPartValue(ActiveDataSourceParameter.name)
  } yield {

    val finalEndpoint = getEndpoint(endpoint)

    val finalActiveDataTab = activeDataTab
    logger.debug(s"Final endpoint: $finalEndpoint")

    val dp = Data(
      data,
      dataUrl,
      dataFile,
      finalEndpoint,
      dataFormat,
      inference,
      targetDataFormat,
      finalActiveDataTab,
      compoundData
    )
    dp
  }

  //  def mkData(partsMap: PartsMap): IO[Data] = for {
  //    data         <- partsMap.optPartValue(DataParameter.name)
  //    compoundData <- partsMap.optPartValue(CompoundDataParameter.name)
  //    dataUrl      <- partsMap.optPartValue(DataURLParameter.name)
  //    dataFile     <- partsMap.optPartValue(DataFileParameter.name)
  //    endpoint     <- partsMap.optPartValue(EndpointParameter.name)
  //    dataFormat <- DataFormat.fromRequestParams(
  //      DataFormatParameter.name,
  //      partsMap
  //    )
  //    inference <- partsMap.optPartValue(InferenceParameter.name)
  //    targetDataFormat <- DataFormat.fromRequestParams(
  //      TargetDataFormatParameter.name,
  //      partsMap
  //    )
  //    activeDataTab <- partsMap.optPartValue(ActiveDataTabParameter.name)
  //  } yield {
  //
  //    val finalEndpoint = getEndpoint(endpoint)
  //
  //    val finalActiveDataTab = activeDataTab
  //    logger.debug(s"Final endpoint: $finalEndpoint")
  //
  //    val dp = Data(
  //      data,
  //      dataUrl,
  //      dataFile,
  //      finalEndpoint,
  //      dataFormat,
  //      inference,
  //      targetDataFormat,
  //      finalActiveDataTab,
  //      compoundData
  //    )
  //    dp
  //  }

  /** @param endpointStr  String containing the endpoint
    * @param endpointRegex Regex used to look for the endpoint in the string
    * @return Optionally, the endpoint contained in a given data string
    */
  private def getEndpoint(
      endpointStr: Option[String],
      endpointRegex: Regex = endpointRegex
  ): Option[String] = {
    endpointStr match {
      case None => None
      case Some(endpoint) =>
        endpoint match {
          case endpointRegex(endpoint) => Some(endpoint)
          case _                       => None
        }

    }
  }

  /** @return Empty data representation, with no inner data and all defaults to none
    */
  def empty: Data =
    Data(
      data = None,
      dataUrl = None,
      dataFile = None,
      optEndpoint = None,
      optDataFormat = None,
      inference = None,
      targetDataFormat = None,
      activeDataTab = None,
      compoundData = None
    )
}

/** Enumeration of the different possible Data sources sent by the client.
  * The source sent indicates the API if the schema was sent in raw text, as a URL
  * to be fetched or as a text file containing the schema.
  * In case the client submits the data in several formats, the selected source will indicate the preferred one.
  */
private[logic] object DataSource extends Enumeration {
  type DataTab = String

  val TEXT     = "#dataTextArea"
  val URL      = "#dataUrl"
  val FILE     = "#dataFile"
  val COMPOUND = "#compoundData"
  val ENDPOINT = "#dataEndpoint"

  val defaultActiveShapeMapTab: DataTab = TEXT
}
