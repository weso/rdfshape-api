package es.weso.server

import Defaults._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import es.weso.rdf.RDFReasoner
import es.weso.schema.{Schema, Schemas}
import scala.io.Source
import scala.util.Try
import org.log4s._

case class SchemaParam(schema: Option[String],
                       schemaURL: Option[String],
                       schemaFile: Option[String],
                       schemaFormatTextArea: Option[String],
                       schemaFormatUrl: Option[String],
                       schemaFormatFile: Option[String],
                       schemaEngine: Option[String],
                       schemaEmbedded: Option[Boolean],
                       targetSchemaEngine: Option[String],
                       targetSchemaFormat: Option[String],
                       activeSchemaTab: Option[String]
                      ) {

  
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

  def getSchema(data: Option[RDFReasoner]): IO[(Option[String], Either[String, Schema])] = {
    getLogger.info(s"SchemaEmbedded: ${schemaEmbedded}")
    val v = schemaEmbedded match {
      case Some(true) => data match {
        case None => IO((None, Left(s"Schema embedded but no data found")))
        case Some(rdf) => for {
          eitherSchema <- Schemas.fromRDF(rdf, schemaEngine.getOrElse(defaultSchemaEngine)).value  
          resp <- eitherSchema match {
            case Left(str) => 
              IO((None, Left(s"Error obtaining schema from RDF $rdf")))
            case Right(schema) => for { 
              str <- schema.serialize(schemaFormat.getOrElse(defaultSchemaFormat))
            } yield (Some(str), Right(schema))
          }
        } yield resp
      }
      case _ => {
        val inputType = activeSchemaTab match {
          case Some(a) => parseSchemaTab(a)
          case None if schema.isDefined => Right(SchemaTextAreaType)
          case None if schemaURL.isDefined => Right(SchemaUrlType)
          case None if schemaFile.isDefined => Right(SchemaFileType)
          case None => Right(SchemaTextAreaType)
        }
        inputType match {
          case Right(`SchemaUrlType`) => {
            schemaURL match {
              case None => IO((None, Left(s"Non value for schemaURL")))
              case Some(schemaUrl) => { 
              val v: IO[Either[Throwable,String]] = IO(Try(Source.fromURL(schemaUrl).mkString).toEither)  
              val e: EitherT[IO,String,(String,Schema)] = for {
                str <- EitherT(v).leftMap(err => s"Error obtaining schema from url $schemaUrl: ${err.getMessage}")
                schema <- Schemas.fromString(
                  str,schemaFormat.getOrElse(defaultSchemaFormat),
                  schemaEngine.getOrElse(defaultSchemaEngine),
                  ApiHelper.getBase).leftMap(s => s"Error parsing contents of $schemaUrl: $s\nContents:\n$str")
              } yield (str,schema)
              e.fold(
                s => ((none[String], s.asLeft[Schema])),
                pair => { 
                  val (str,schema) = pair
                  ((Some(str), Right(schema)))
                }
              )
            }
           }
          }
          case Right(`SchemaFileType`) => {
            schemaFile match {
              case None => IO((None, Left(s"No value for schemaFile")))
              case Some(schemaStr) =>
                val schemaFormatStr = schemaFormat.getOrElse(defaultSchemaFormat)
                val schemaEngineStr = schemaEngine.getOrElse(defaultSchemaEngine)
                Schemas.fromString(schemaStr, schemaFormatStr, schemaEngineStr, ApiHelper.getBase).fold(
                  s => (Some(schemaStr), Left(s"Error parsing file: $s")),
                  schema => (Some(schemaStr), Right(schema))
                )
            }
          }
          case Right(`SchemaTextAreaType`) => {
            val schemaStr = schema.getOrElse("")
            Schemas.fromString(schemaStr,
              schemaFormat.getOrElse(defaultSchemaFormat),
              schemaEngine.getOrElse(defaultSchemaEngine),
              ApiHelper.getBase).fold(
                s => (Some(schemaStr), Left(s)), 
                schema => (Some(schemaStr), Right(schema))
              ) 
          }
          case Right(other) => IO((None, Left(s"Unknown value for activeSchemaTab: $other")))
          case Left(msg) => IO((None, Left(msg)))
        }
      }
    }
   println(s"getSchema: Result: ${v}") 
   v 
  }


}

object SchemaParam {

  private[server] def mkSchema[F[_]:Effect](partsMap: PartsMap[F],
                               data: Option[RDFReasoner]
                      ): EitherT[F, String, (Schema, SchemaParam)] = {
    val L = implicitly[LiftIO[F]]
    val r: F[Either[String, (Schema,SchemaParam)]] = for {
      sp <- {
        getLogger.info(s"PartsMap: $partsMap")
        mkSchemaParam(partsMap)
      }
      eitherPair <- L.liftIO(sp.getSchema(data).attempt)
      resp <- eitherPair.fold(
        s => Monad[F].pure(Left(s"Error: $s")), 
        pair => {
          val (maybeStr, maybeSchema) = pair
          maybeSchema match {
            case Left(str) => Monad[F].pure(Left(str))
            case Right(schema) => Monad[F].pure(Right((schema, sp.copy(schema = maybeStr)))) 
          }
        })
      } yield resp
    EitherT(r)
  }

  private[server] def mkSchemaParam[F[_]:Effect](partsMap: PartsMap[F]): F[SchemaParam] = for {
    schema <- partsMap.optPartValue("schema")
    schemaURL <- partsMap.optPartValue("schemaURL")
    schemaFile <- partsMap.optPartValue("schemaFile")
    schemaFormatTextArea <- partsMap.optPartValue("schemaFormatTextArea")
    schemaFormatUrl <- partsMap.optPartValue("schemaFormatUrl")
    schemaFormatFile <- partsMap.optPartValue("schemaFormatFile")
    schemaEngine <- partsMap.optPartValue("schemaEngine")
    targetSchemaEngine <- partsMap.optPartValue("targetSchemaEngine")
    targetSchemaFormat <- partsMap.optPartValue("targetSchemaFormat")
    activeSchemaTab <- partsMap.optPartValue("activeSchemaTab")
    schemaEmbedded <- partsMap.optPartValueBoolean("schemaEmbedded")
  } yield {
    println(s"mkSchemaParam => schemaURL = ${schemaURL}")
    SchemaParam(schema, schemaURL, schemaFile,
      schemaFormatTextArea, schemaFormatUrl, schemaFormatFile,
      schemaEngine, schemaEmbedded,
      targetSchemaEngine, targetSchemaFormat, activeSchemaTab
    )
  }

  private[server] def empty: SchemaParam =
    SchemaParam(None,None,None,None,None,None,None,None,None,None,None)

}