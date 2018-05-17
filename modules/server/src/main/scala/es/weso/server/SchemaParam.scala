package es.weso.server

import Defaults._
import cats.data.EitherT
import cats.effect.IO
import es.weso.rdf.RDFReasoner
import es.weso.schema.{Schema, Schemas}

import scala.io.Source
import scala.util.Try
import org.log4s.getLogger

case class SchemaParam(schema: Option[String],
                       schemaURL: Option[String],
                       schemaFile: Option[String],
                       schemaFormatTextArea: Option[String],
                       schemaFormatUrl: Option[String],
                       schemaFormatFile: Option[String],
                       schemaEngine: Option[String],
                       schemaEmbedded: Option[Boolean],
                       targetSchemaFormat: Option[String],
                       activeSchemaTab: Option[String]
                      ) {
  private val logger = getLogger
  
  sealed abstract class SchemaInputType {
    val id: String
  }
  case object SchemaUrlType extends SchemaInputType {
    override val id = "#schemaUrl"
  }
  case object SchemaFileType extends SchemaInputType {
    override val id = "#schemaFile"
  }
  case object SchemaTextAreaType extends SchemaInputType {
    override val id = "#schemaTextArea"
  }

  def parseSchemaTab(tab: String): Either[String,SchemaInputType] = {
    val inputTypes = List(SchemaUrlType,SchemaFileType,SchemaTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None => Left(s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]")
    }
  }

  val schemaFormat: Option[String] =
    parseSchemaTab(activeSchemaTab.getOrElse(defaultActiveSchemaTab)) match {
      case Right(`SchemaUrlType`) => schemaFormatUrl
      case Right(`SchemaFileType`) => schemaFormatFile
      case Right(`SchemaTextAreaType`) => schemaFormatTextArea
      case _ => None
    }

  private def chooseSchemaTab: String = {
    (schema, schemaURL) match {
      case (Some(_),None) => SchemaTextAreaType.id
      case (None,Some(_)) => SchemaUrlType.id
      case (None,None) => defaultActiveSchemaTab
      case (Some(_),Some(_)) => defaultActiveSchemaTab
    }
  }

  def getSchema(data: Option[RDFReasoner]): (Option[String], Either[String, Schema]) = {
    logger.info(s"SchemaEmbedded: ${schemaEmbedded}")
    schemaEmbedded match {
      case Some(true) => data match {
        case None => (None, Left(s"Schema embedded but no data found"))
        case Some(rdf) => Schemas.fromRDF(rdf, schemaEngine.getOrElse(defaultSchemaEngine)) match {
          case Left(str) => (None, Left(s"Error obtaining schema from RDF $rdf"))
          case Right(schema) => schema.serialize(schemaFormat.getOrElse(defaultSchemaFormat)) match {
            case Left(str) => (None, Right(schema))
            case Right(str) => (Some(str), Right(schema))
          }
        }
      }
      case _ => {
        logger.info(s"######## Schema not embedded...Active schema tab: ${activeSchemaTab}")
        parseSchemaTab(activeSchemaTab.getOrElse(chooseSchemaTab)) match {
          case Right(`SchemaUrlType`) => {
            logger.info(s"######## SchemaUrl: ${schemaURL}")
            schemaURL match {
              case None => (None, Left(s"Non value for dataURL"))
              case Some(schemaUrl) => Try {
                // val uri = new java.net.URI(schemaUrl)
                Source.fromURL(schemaUrl).mkString
              }.toEither match {
                case Left(err) => (None, Left(s"Error obtaining schema from url $schemaUrl: ${err.getMessage} "))
                case Right(schemaStr) => Schemas.fromString(schemaStr,
                  schemaFormat.getOrElse(defaultSchemaFormat),
                  schemaEngine.getOrElse(defaultSchemaEngine),
                  ApiHelper.getBase) match {
                  case Left(msg) => (Some(schemaStr), Left(s"Error parsing file: $msg"))
                  case Right(schema) => (Some(schemaStr), Right(schema))
                }
              }
            }
          }
          case Right(`SchemaFileType`) => {
            schemaFile match {
              case None => (None, Left(s"No value for schemaFile"))
              case Some(schemaStr) =>
                val schemaFormatStr = schemaFormat.getOrElse(defaultSchemaFormat)
                val schemaEngineStr = schemaEngine.getOrElse(defaultSchemaEngine)
                Schemas.fromString(schemaStr, schemaFormatStr, schemaEngineStr, ApiHelper.getBase) match {
                  case Left(msg) => (Some(schemaStr), Left(s"Error parsing file: $msg"))
                  case Right(schema) => (Some(schemaStr), Right(schema))
                }
            }
          }
          case Right(`SchemaTextAreaType`) => {
            val schemaStr = schema.getOrElse("")
            Schemas.fromString(schemaStr,
              schemaFormat.getOrElse(defaultSchemaFormat),
              schemaEngine.getOrElse(defaultSchemaEngine),
              ApiHelper.getBase) match {
                case Left(msg) => (Some(schemaStr), Left(msg))
                case Right(schema) => (Some(schemaStr), Right(schema))
            }
          }
          case Right(other) => (None, Left(s"Unknown value for activeSchemaTab: $other"))
          case Left(msg) => (None, Left(msg))
        }
      }
    }
  }
}

object SchemaParam {

  private val logger = getLogger

  private[server] def mkSchema(partsMap: PartsMap,
                               data: Option[RDFReasoner]
                      ): EitherT[IO, String, (Schema, SchemaParam)] = {
    val r = for {
      sp <- {
        logger.info(s"PartsMap: $partsMap")
        mkSchemaParam(partsMap)
      }
    } yield {
      logger.info(s"SchemaParam: $sp")
      val (maybeStr, maybeSchema) = sp.getSchema(data)
      maybeSchema match {
        case Left(str) => Left(str)
        case Right(schema) => Right((schema, sp.copy(schema = maybeStr)))
      }
    }
    EitherT(r)
  }

  private[server] def mkSchemaParam(partsMap: PartsMap): IO[SchemaParam] = for {
    schema <- partsMap.optPartValue("schema")
    schemaURL <- partsMap.optPartValue("schemaURL")
    schemaFile <- partsMap.optPartValue("schemaFile")
    schemaFormatTextArea <- partsMap.optPartValue("schemaFormatTextArea")
    schemaFormatUrl <- partsMap.optPartValue("schemaFormatUrl")
    schemaFormatFile <- partsMap.optPartValue("schemaFormatFile")
    schemaEngine <- partsMap.optPartValue("schemaEngine")
    targetSchemaFormat <- partsMap.optPartValue("targetSchemaFormat")
    activeSchemaTab <- partsMap.optPartValue("activeSchemaTab")
    schemaEmbedded <- partsMap.optPartValueBoolean("schemaEmbedded")
  } yield
    SchemaParam(schema, schemaURL, schemaFile,
      schemaFormatTextArea, schemaFormatUrl, schemaFormatFile,
      schemaEngine, schemaEmbedded, targetSchemaFormat, activeSchemaTab)

}