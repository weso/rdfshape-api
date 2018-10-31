package es.weso.server

import java.net.URI

import Defaults._
import cats.data.EitherT
import cats.effect.IO
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import org.http4s.Uri
import org.http4s.client.blaze.Http1Client
import org.log4s.getLogger

case class DataParam(data: Option[String],
                     dataURL: Option[String],
                     dataFile: Option[String],
                     endpoint: Option[String],
                     dataFormatTextarea: Option[String],
                     dataFormatUrl: Option[String],
                     dataFormatFile: Option[String],
                     inference: Option[String],
                     targetDataFormat: Option[String],
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

  val dataFormat: Option[String] = parseDataTab(activeDataTab.getOrElse(defaultActiveDataTab)) match {
    case Right(`dataUrlType`) => dataFormatUrl
    case Right(`dataFileType`) => dataFormatFile
    case Right(`dataTextAreaType`) => dataFormatTextarea
    case _ => None
  }

  private def applyInference(rdf: RDFReasoner, inference: Option[String], dataFormat: String): (Option[String],Either[String,RDFReasoner]) = {
    extendWithInference(rdf, inference) match {
      case Left(msg) => (rdf.serialize(dataFormat).toOption, Left(s"Error applying inference: $msg"))
      case Right(newRdf) => (newRdf.serialize(dataFormat).toOption, Right(newRdf))
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
  def getData: (Option[String], Either[String,RDFReasoner]) = {
    logger.debug(s"ActiveDataTab: $activeDataTab")
    val inputType = activeDataTab match {
      case None => {
        if (endpoint.isDefined) Right(dataEndpointType)
        else Right(dataTextAreaType)
      }
      case Some(a) => parseDataTab(a)
    }
    logger.debug(s"Input type: $inputType")
    inputType match {
      case Right(`dataUrlType`) => {
        dataURL match {
          case None => (None, Left(s"Non value for dataURL"))
          case Some(dataUrl) => {
            val dataFormat = dataFormatUrl.getOrElse(defaultDataFormat)
            rdfFromUri(new URI(dataUrl), dataFormat,None) match {
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
            rdfFromString(dataStr, dataFormat, None) match {
              case Left(msg) => (Some(dataStr), Left(msg))
              case Right(rdf) => {
                extendWithInference(rdf, inference) match {
                  case Left(msg) => (rdf.serialize(dataFormat).toOption, Left(s"Error applying inference: $msg"))
                  case Right(newRdf) => (newRdf.serialize(dataFormat).toOption, Right(newRdf))
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
        data match {
          case None => (None, Right(RDFAsJenaModel.empty))
          case Some(data) => {
            val dataFormat = dataFormatTextarea.getOrElse(defaultDataFormat)
            rdfFromString(data, dataFormat, None) match {
              case Left(msg) => (Some(data), Left(msg))
              case Right(rdf) => {
                extendWithInference(rdf, inference) match {
                  case Left(msg) => (rdf.serialize(dataFormat).toOption, Left(s"Error applying inference: $msg"))
                  case Right(newRdf) => (newRdf.serialize(dataFormat).toOption, Right(newRdf))
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
                            format: String,
                            base: Option[String]
                           ): Either[String, RDFReasoner] = {
    format.toLowerCase match {
      case "html" | "html-rdfa" | "html-microdata" => HTML2RDF.extractFromString(str,format.toLowerCase())
      case _ => RDFAsJenaModel.fromChars(str,format,base)
    }
  }

  private def rdfFromUri(uri: URI,
                         format: String,
                         base: Option[String]
                        ): Either[String, RDFReasoner] = {
    format.toLowerCase match {
      case "html" | "html-rdfa" | "html-microdata" => HTML2RDF.extractFromUrl(uri.toString, format.toLowerCase())
      case _ => RDFAsJenaModel.fromURI(uri.toString, format, base)
    }
  }

}

object DataParam {
  private[this] val logger = getLogger

  private[server] def mkData(partsMap: PartsMap
                            ): EitherT[IO,String,(RDFReasoner,DataParam)] = {

    val r = for {
      dp <- mkDataParam(partsMap)
    } yield {
      val (maybeStr, maybeData) = dp.getData
      maybeData match {
        case Left(str) => Left(str)
        case Right(data) => Right((data, dp.copy(data = maybeStr)))
      }
    }
    EitherT(r)
  }

  private[server] def mkDataParam(partsMap: PartsMap): IO[DataParam] = for {
    data <- partsMap.optPartValue("data")
    dataURL <- partsMap.optPartValue("dataURL")
    dataFile <- partsMap.optPartValue("dataFile")
    endpoint <- partsMap.optPartValue("endpoint")
    dataFormatTextArea <- partsMap.optPartValue("dataFormatTextArea")
    dataFormatUrl <- partsMap.optPartValue("dataFormatUrl")
    dataFormatFile <- partsMap.optPartValue("dataFormatFile")
    inference <- partsMap.optPartValue("inference")
    targetDataFormat <- partsMap.optPartValue("targetDataFormat")
    activeDataTab <- partsMap.optPartValue("rdfDataActiveTab")
  } yield {
    logger.debug(s"<<<***Data: $data")
    logger.debug(s"<<<***Data Format TextArea: $dataFormatTextArea")
    logger.debug(s"<<<***Data Format Url: $dataFormatUrl")
    logger.debug(s"<<<***Data Format File: $dataFormatFile")
    logger.debug(s"<<<***Data URL: $dataURL")
    logger.debug(s"<<<***Endpoint: $endpoint")
    logger.debug(s"<<<***ActiveDataTab: $activeDataTab")
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
    logger.debug(s"<<<***Endpoint: $finalEndpoint")

    DataParam(data,dataURL,dataFile,finalEndpoint,
      dataFormatTextArea,dataFormatUrl,dataFormatFile,
      inference,targetDataFormat,finalActiveDataTab
    )
  }

  private[server] def empty: DataParam =
    DataParam(None,None,None,None,None,None,None,None,None,None)


}