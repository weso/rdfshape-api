package es.weso.server

import java.net.URI

import Defaults._
import cats.data.EitherT
import cats.implicits._
import cats.effect.{Effect, IO}
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.server.helper.DataFormat
import org.log4s.getLogger
import es.weso.utils.IOUtils._

case class DataParam(data: Option[String],
                     dataURL: Option[String],
                     dataFile: Option[String],
                     maybeEndpoint: Option[String],
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
                            ): IO[RDFReasoner] = for {
    newRdf <- extendWithInference(rdf, inference) 
//    str <- rdf.serialize(dataFormat.name)
  } yield newRdf

  private def extendWithInference(rdf: RDFReasoner,
                                  optInference: Option[String]
                                 ): IO[RDFReasoner] = {
    logger.debug(s"############# Applying inference $optInference")
    optInference.fold(IO(rdf))(rdf.applyInference(_)) /*.fold(
      msg => Left(s"Error applying inference to RDF: $msg"),
      (newRdf: RDFReasoner) => Right(newRdf)
    ) */
  }

//  private def err[A](msg:String): EitherT[IO,String,A] = EitherT.fromEither(msg.asLeft[A])
//  private def fromIO[A](io:IO[A]): EitherT[IO,String,A] = EitherT.liftF(io)

  /**
    * get RDF data from data parameters
    * @return a pair where the first value can be Some(string)
    *         if it has string representation and the second parameter
    *         is the RDF data
    */
  def getData(relativeBase: Option[IRI]
  ): ESIO[(Option[String], RDFReasoner)] = {
    val base = relativeBase.map(_.str)
    println(s"ActiveDataTab: $activeDataTab")
    val inputType = activeDataTab match {
      case Some(a) => parseDataTab(a)
      case None if data.isDefined => Right(dataTextAreaType)
      case None if dataURL.isDefined => Right(dataUrlType)
      case None if dataFile.isDefined => Right(dataFileType)
      case None if maybeEndpoint.isDefined => Right(dataEndpointType)
      case None => Right(dataTextAreaType)
    }
    println(s"Input type: $inputType")
    inputType match {
      case Right(`dataUrlType`) => {
        dataURL match {
          case None => fail_es(s"Non value for dataURL")
          case Some(dataUrl) => {
            val dataFormat = dataFormatUrl.getOrElse(DataFormat.default)
            for {
              rdf <- rdfFromUri(new URI(dataUrl), dataFormat,base)
              newRdf <- io2es(applyInference(rdf, inference, dataFormat))
              eitherStr <- io2es(newRdf.serialize(dataFormat.name,None).attempt)
              optStr = eitherStr.toOption
            } yield (optStr, newRdf)

/*            rdfFromUri(new URI(dataUrl), dataFormat,base) match {
              case Left(str) => err(s"Error obtaining $dataUrl with $dataFormat: $str")
              case Right(rdf) => io2es(
                for { 
                  newRdf <- applyInference(rdf, inference, dataFormat)
                  eitherStr <- newRdf.serialize(dataFormat.name,None).attempt
                  optStr = eitherStr.toOption
                } yield (optStr, newRdf)
              ) 
            } */
          } 
        }
      }
      case Right(`dataFileType`) => {
        dataFile match {
          case None => fail_es(s"No value for dataFile")
          case Some(dataStr) =>
            val dataFormat = dataFormatFile.getOrElse(defaultDataFormat)
            io2es(for {
              iriBase <- mkBase(base)
              rdf <- RDFAsJenaModel.fromString(dataStr, dataFormat.name, iriBase)
              newRdf <- extendWithInference(rdf, inference)
              eitherStr <- newRdf.serialize(dataFormat.name,None).attempt
              optStr = eitherStr.toOption              
            } yield (optStr,newRdf))
        }
      }

      case Right(`dataEndpointType`) => {
        maybeEndpoint match {
          case None => fail_es(s"No value for endpoint")
          case Some(endpointUrl) => {
            for {
              endpoint <- io2es(Endpoint.fromString(endpointUrl))
              newRdf <- io2es(extendWithInference(endpoint, inference))
            } yield (None,newRdf)
          }
        }
      }
      case Right(`dataTextAreaType`) => {
        println(s"Obtaining data from textArea")
        data match {
          case None => for {
            empty <- io2es(RDFAsJenaModel.empty)
          } yield (None, empty)
          // fromIO(RDFAsJenaModel.empty)
          case Some(data) => {
            val dataFormat = dataFormatTextarea.getOrElse(dataFormatValue.getOrElse(defaultDataFormat))
            for {
              rdf <- rdfFromString(data, dataFormat, base)
              newRdf <- io2es(extendWithInference(rdf, inference))
              eitherStr <- io2es(newRdf.serialize(dataFormat.name,None).attempt)
              optStr = eitherStr.toOption
            } yield (optStr,newRdf)
          }}}
/*            rdfFromString(data, dataFormat, base) match {
              case Left(msg) => (Some(data), Left(msg))
              case Right(rdf) => {
                extendWithInference(rdf, inference) match {
                  case Left(msg) => err(s"Error applying inference: $msg")
                  case Right(newRdf) => {
                    val maybeRdfStr = newRdf.serialize(dataFormat.name,relativeBase).toOption
                    maybeEndpoint match {
                      case None =>
                        (maybeRdfStr, Right(newRdf))
                      case Some(endpoint) =>
                        Endpoint.fromString(endpoint) match {
                          case Left(msg) => err(s"Error applying inference: $msg")
                          case Right(endpoint) => Compound(List(endpoint, newRdf))
                        }
                    }
                  }
                }
              }
            }
          }
        } */
      
      case Right(other) => fail_es(s"Unknown value for activeDataTab: $other")
      case Left(msg) => fail_es(msg)
    }
  }


  private def rdfFromString(str: String,
                            format: DataFormat,
                            base: Option[String]
                           ): ESIO[RDFReasoner] = {
    println(s"Format: $format")
    format.name match {
      case f if HTML2RDF.availableExtractorNames contains f => for {
        eitherRdf <- HTML2RDF.extractFromString(str,f)
      } yield eitherRdf
      case _ => for {
        baseIri <- io2es(mkBase(base))
        rdf <- io2es(RDFAsJenaModel.fromChars(str,format.name,baseIri))
      } yield rdf
    }
  }

  private def rdfFromUri(uri: URI,
                         format: DataFormat,
                         base: Option[String]
                        ): ESIO[RDFReasoner] = {
    format.name.toLowerCase match {
      case f if HTML2RDF.availableExtractorNames contains f =>
        HTML2RDF.extractFromUrl(uri.toString, f)
      case _ => for {
       baseIri <- io2es(mkBase(base))
       rdf <- io2es(RDFAsJenaModel.fromURI(uri.toString, format.name, baseIri))
      } yield rdf
    }
  }

  private def mkBaseIri(maybeBase: Option[String]): Either[String, Option[IRI]] = maybeBase match {
    case None => Right(None)
    case Some(str) => IRI.fromString(str).map(Some(_))
  }

   private def mkBase(base: Option[String]): IO[Option[IRI]] = base match {
    case None => IO(None)
    case Some(str) => IRI.fromString(str).fold(e => 
      IO.raiseError(new RuntimeException(s"Cannot get IRI from $str")),
      (iri: IRI) => IO(Some(iri))
    )
  }

}

object DataParam {
  private[this] val logger = getLogger

  private[server] def mkData[F[_]:Effect](
     partsMap: PartsMap[F],
     relativeBase: Option[IRI]
    ): ESF[(RDFReasoner,DataParam),F] = {

    val r: ESF[(RDFReasoner, DataParam),F] = for {
      dp <- f2es(mkDataParam[F](partsMap))
      pair <- esio2esf(dp.getData(relativeBase))
    } yield {
      val (optStr, rdf) = pair
      (rdf, dp.copy(data = optStr))
    }
    r
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

  private[server] def mkDataParam[F[_]:Effect](partsMap: PartsMap[F]
  ): F[DataParam] = for {
    data <- partsMap.optPartValue("data")
    dataURL <- partsMap.optPartValue("dataURL")
    dataFile <- partsMap.optPartValue("dataFile")
    endpoint <- partsMap.optPartValue("endpoint")
    dataFormatTextArea <- getDataFormat("dataFormatTextArea", partsMap)
    dataFormatUrl <- getDataFormat("dataFormatUrl",partsMap)
    dataFormatFile <- getDataFormat("dataFormatFile", partsMap)
    dataFormatValue <- getDataFormat("dataFormat", partsMap)
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