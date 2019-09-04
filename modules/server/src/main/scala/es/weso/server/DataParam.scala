package es.weso.server

import java.net.URI

import Defaults._
import cats.data.EitherT
import cats.implicits._
import cats.effect.{Effect, IO}
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import es.weso.rdf.nodes.IRI
import es.weso.server.helper.DataFormat
import org.log4s.getLogger

case class DataParam(data: Option[String],
                     dataURL: Option[String],
                     dataFile: Option[String],
                     endpoint: Option[String],
                     dataFormatValue: Option[DataFormat],
                     dataFormatTextarea: Option[DataFormat],
                     dataFormatUrl: Option[DataFormat],
                     dataFormatFile: Option[DataFormat],
                     inference: Option[String],
                     targetDataFormat: Option[DataFormat],
                     activeDataTab: Option[String]
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

  def parseDataTab(tab: String): Either[String, DataInputType] = {
    logger.debug(s"parseDataTab: tab = $tab")
    val inputTypes = List(dataUrlType,dataFileType,dataEndpointType,dataTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None => Left(s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]")
    }
  }

  val dataFormat: Option[DataFormat] = parseDataTab(activeDataTab.getOrElse(defaultActiveDataTab)) match {
    case Right(`dataUrlType`) => dataFormatUrl
    case Right(`dataFileType`) => dataFormatFile
    case Right(`dataTextAreaType`) => dataFormatTextarea
    case _ => dataFormatValue
  }

  private def applyInference(rdf: RDFReasoner,
                             inference: Option[String],
                             dataFormat: DataFormat
                            ): (Option[String],Either[String,RDFReasoner]) = {
    extendWithInference(rdf, inference) match {
      case Left(msg) => (rdf.serialize(dataFormat.name).toOption, Left(s"Error applying inference: $msg"))
      case Right(newRdf) => (newRdf.serialize(dataFormat.name).toOption, Right(newRdf))
    }
  }

  private def extendWithInference(rdf: RDFReasoner,
                                  optInference: Option[String]
                                 ): Either[String,RDFReasoner] = {
    logger.debug(s"############# Applying inference $optInference")
    rdf.applyInference(optInference.getOrElse("None")).fold(
      msg => Left(s"Error applying inference to RDF: $msg"),
      (newRdf: RDFReasoner) => Right(newRdf)
    )
  }

  /**
    * get RDF data from data parameters
    * @return a pair where the first value can be Some(string)
    *         if it has string representation and the second parameter
    *         is the RDF data
    */
  def getData(relativeBase: Option[IRI]): (Option[String], Either[String,RDFReasoner]) = {
    val base = relativeBase.map(_.str)
    println(s"ActiveDataTab: $activeDataTab")
    val inputType = activeDataTab match {
      case Some(a) => parseDataTab(a)
      case None if endpoint.isDefined => Right(dataEndpointType)
      case None if data.isDefined => Right(dataTextAreaType)
      case None if dataURL.isDefined => Right(dataUrlType)
      case None if dataFile.isDefined => Right(dataFileType)
      case None => Right(dataTextAreaType)
    }
    println(s"Input type: $inputType")
    inputType match {
      case Right(`dataUrlType`) => {
        dataURL match {
          case None => (None, Left(s"Non value for dataURL"))
          case Some(dataUrl) => {
            val dataFormat = dataFormatUrl.getOrElse(DataFormat.default)
            rdfFromUri(new URI(dataUrl), dataFormat,base) match {
              case Left(str) => (None, Left(s"Error obtaining $dataUrl with $dataFormat: $str"))
              case Right(rdf) => applyInference(rdf, inference, dataFormat)
            }
          }
        }
      }
      case Right(`dataFileType`) => {
        dataFile match {
          case None => (None, Left(s"No value for dataFile"))
          case Some(dataStr) =>
            val dataFormat = dataFormatFile.getOrElse(defaultDataFormat)
            rdfFromString(dataStr, dataFormat, base) match {
              case Left(msg) => (Some(dataStr), Left(msg))
              case Right(rdf) => {
                extendWithInference(rdf, inference) match {
                  case Left(msg) => (rdf.serialize(dataFormat.name).toOption, Left(s"Error applying inference: $msg"))
                  case Right(newRdf) => (newRdf.serialize(dataFormat.name).toOption, Right(newRdf))
                }
              }
            }
        }
      }
      case Right(`dataEndpointType`) => {
        endpoint match {
          case None => (None, Left(s"No value for endpoint"))
          case Some(endpointUrl) => {
            Endpoint.fromString(endpointUrl) match {
              case Left(str) => (None, Left(s"Error creating endpoint: $endpointUrl"))
              case Right(endpoint) => {
                (None, extendWithInference(endpoint, inference))
              }
            }
          }
        }
      }
      case Right(`dataTextAreaType`) => {
        println(s"Obtaining data from textArea")
        data match {
          case None => (None, Right(RDFAsJenaModel.empty))
          case Some(data) => {
            val dataFormat = dataFormatTextarea.getOrElse(dataFormatValue.getOrElse(defaultDataFormat))
            rdfFromString(data, dataFormat, base) match {
              case Left(msg) => (Some(data), Left(msg))
              case Right(rdf) => {
                extendWithInference(rdf, inference) match {
                  case Left(msg) => (Some(data), Left(s"Error applying inference: $msg"))
                  case Right(newRdf) => (newRdf.serialize(dataFormat.name,relativeBase).toOption, Right(newRdf))
                }
              }
            }
          }
        }
      }
      case Right(other) => (None, Left(s"Unknown value for activeDataTab: $other"))
      case Left(msg) => (None, Left(msg))
    }
  }

  private def rdfFromString(str: String,
                            format: DataFormat,
                            base: Option[String]
                           ): Either[String, RDFReasoner] = {
    println(s"Format: $format")
    format.name match {
      case f if HTML2RDF.availableExtractorNames contains f => {
        println(s"From HTML with format $f")
        HTML2RDF.extractFromString(str,f)
      }
      case _ => for {
        baseIri <- mkBaseIri(base)
        rdf <- RDFAsJenaModel.fromChars(str,format.name,baseIri)
      } yield rdf
    }
  }

  private def rdfFromUri(uri: URI,
                         format: DataFormat,
                         base: Option[String]
                        ): Either[String, RDFReasoner] = {
    format.name.toLowerCase match {
      case f if HTML2RDF.availableExtractorNames contains f =>
        HTML2RDF.extractFromUrl(uri.toString, f)
      case _ => for {
       baseIri <- mkBaseIri(base)
       rdf <- RDFAsJenaModel.fromURI(uri.toString, format.name, baseIri)
      } yield rdf
    }
  }

  private def mkBaseIri(maybeBase: Option[String]): Either[String, Option[IRI]] = maybeBase match {
    case None => Right(None)
    case Some(str) => IRI.fromString(str).map(Some(_))
  }
}

object DataParam {
  private[this] val logger = getLogger

  private[server] def mkData[F[_]:Effect](partsMap: PartsMap[F],
                             relativeBase: Option[IRI]
                            ): EitherT[F,String,(RDFReasoner,DataParam)] = {

    val r = for {
      dp <- mkDataParam[F](partsMap)
    } yield {
      val (maybeStr, maybeData) = dp.getData(relativeBase)
      maybeData match {
        case Left(str) => Left(str)
        case Right(data) => Right((data, dp.copy(data = maybeStr)))
      }
    }
    EitherT(r)
  }

  private def getDataFormat[F[_]](name: String, partsMap: PartsMap[F])(implicit F: Effect[F]): F[Option[DataFormat]] = for {
    maybeStr <- partsMap.optPartValue(name)
  } yield maybeStr match {
    case None => None
    case Some(str) => DataFormat.fromString(str).fold(
      err => {
        logger.error(s"Unsupported dataFormat: $str")
        None
      },
      df => Some(df)
    )
  }

  private[server] def mkDataParam[F[_]:Effect](partsMap: PartsMap[F]): F[DataParam] = for {
    data <- partsMap.optPartValue("data")
    dataURL <- partsMap.optPartValue("dataURL")
    dataFile <- partsMap.optPartValue("dataFile")
    endpoint <- partsMap.optPartValue("endpoint")
    dataFormatTextArea <- getDataFormat("dataFormatTextArea", partsMap)
    dataFormatValue <- getDataFormat("dataFormat", partsMap)
    dataFormatUrl <- getDataFormat("dataFormatUrl",partsMap)
    dataFormatFile <- getDataFormat("dataFormatFile", partsMap)
    inference <- partsMap.optPartValue("inference")
    targetDataFormat <- getDataFormat("targetDataFormat",partsMap)
    activeDataTab <- partsMap.optPartValue("rdfDataActiveTab")
  } yield {
    println(s"<<<***Data: $data")
    println(s"<<<***Data Format: $dataFormatValue")
    println(s"<<<***Data Format TextArea: $dataFormatTextArea")
    println(s"<<<***Data Format Url: $dataFormatUrl")
    println(s"<<<***Data Format File: $dataFormatFile")
    println(s"<<<***Data URL: $dataURL")
    println(s"<<<***Endpoint: $endpoint")
    println(s"<<<***ActiveDataTab: $activeDataTab")
    val endpointRegex = "Endpoint: (.+)".r
    val finalEndpoint = endpoint.fold(data match {
      case None => None
      case Some(str) => str match {
        case endpointRegex(ep) => Some(ep)
        case _ => None
      }
    })(Some(_))
    val finalActiveDataTab = activeDataTab /* finalEndpoint match {
      case Some(endpoint) =>
        if (endpoint.length > 0) Some("#dataEndpoint")
        else activeDataTab
      case None => activeDataTab
    } */
    println(s"<<<***Endpoint: $finalEndpoint")

    DataParam(data,dataURL,dataFile,finalEndpoint,dataFormatValue,
      dataFormatTextArea,dataFormatUrl,dataFormatFile,
      inference,targetDataFormat,finalActiveDataTab
    )
  }

  private[server] def empty: DataParam =
    DataParam(None,None,None,None,None,None,None,None,None,None,None)


}