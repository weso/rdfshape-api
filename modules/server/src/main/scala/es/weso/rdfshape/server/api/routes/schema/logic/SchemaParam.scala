package es.weso.rdfshape.server.api.routes.schema.logic

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdfshape.server.api.definitions.ApiDefaults.{
  defaultSchemaEngine,
  defaultSchemaFormat
}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaOperations.getBase
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{Schema, Schemas}

import scala.io.Source
import scala.util.Try

sealed case class SchemaParam(
    schema: Option[String],
    schemaURL: Option[String],
    schemaFile: Option[String],
    optSchemaFormat: Option[SchemaFormat],
    schemaEngine: Option[String],
    targetSchemaEngine: Option[String],
    targetSchemaFormat: Option[String],
    activeSchemaTab: Option[String]
) extends LazyLogging {

  val schemaFormat: SchemaFormat =
    optSchemaFormat.getOrElse(defaultSchemaFormat)

  def getSchema(
      data: Option[RDFReasoner]
  ): IO[(Option[String], Either[String, Schema])] = {

    logger.debug(s"activeSchemaTab: $activeSchemaTab")
    logger.debug(s"schemaEngine: $schemaEngine")
    val inputType = activeSchemaTab match {
      case Some(a)                      => parseSchemaTab(a)
      case None if schema.isDefined     => Right(SchemaTextAreaType)
      case None if schemaURL.isDefined  => Right(SchemaUrlType)
      case None if schemaFile.isDefined => Right(SchemaFileType)
      case None                         => Right(SchemaTextAreaType)
    }
    logger.debug(s"inputType: $inputType")
    val maybeSchema: IO[(Option[String], Either[String, Schema])] =
      inputType match {
        case Right(`SchemaUrlType`) =>
          logger.debug("Schema input type - SchemaUrlType")
          schemaURL match {
            case None => IO((None, Left(s"Non value for schemaURL")))
            case Some(schemaUrl) =>
              val e: IO[(String, Schema)] = for {
                str <- IO.fromEither(
                  Try(Source.fromURL(schemaUrl).mkString).toEither
                )
                schema <- Schemas.fromString(
                  str,
                  schemaFormat.name,
                  schemaEngine.getOrElse(defaultSchemaEngine),
                  getBase
                )
                _ <- IO {
                  logger.debug("Schema parsed")
                }
              } yield (str, schema)
              e.attempt.map(
                _.fold(
                  s => (none[String], s.getMessage.asLeft[Schema]),
                  pair => {
                    val (str, schema) = pair
                    (Some(str), Right(schema))
                  }
                )
              )
          }
        case Right(`SchemaFileType`) =>
          logger.debug("Schema input type - SchemaFileType")
          schemaFile match {
            case None => IO((None, Left(s"No value for schemaFile")))
            case Some(schemaStr) =>
              val schemaFormatStr =
                schemaFormat.name
              val schemaEngineStr =
                schemaEngine.getOrElse(defaultSchemaEngine)
              Schemas
                .fromString(
                  schemaStr,
                  schemaFormatStr,
                  schemaEngineStr,
                  getBase
                )
                .attempt
                .map(
                  _.fold(
                    s => (Some(schemaStr), Left(s"Error parsing file: $s")),
                    schema => (Some(schemaStr), Right(schema))
                  )
                )
          }
        case Right(`SchemaTextAreaType`) =>
          logger.debug("Schema input type - SchemaTextAreaType")
          val schemaStr = schema.getOrElse("")
          for {
            pair <- Schemas
              .fromString(
                schemaStr,
                schemaFormat.name,
                schemaEngine.getOrElse(defaultSchemaEngine),
                getBase
              )
              .attempt
              .map(
                _.fold(
                  err => {
                    /* TODO: some specific malformed schemas produce a
                     * NullPointerException with no further message */
                    val msg =
                      if(err.getMessage == null) "Unknown error."
                      else err.getMessage
                    (Some(schemaStr), Left(msg))
                  },
                  schema => (Some(schemaStr), Right(schema))
                )
              )
            (str, eitherSchema) = pair
            nameSchema          = eitherSchema.map(_.name).getOrElse(s"No schema")
            _ <- IO {
              logger.debug(s"nameSchema: $nameSchema")
            }
            foundSchema <- Schemas.lookupSchema(
              schemaEngine.getOrElse(defaultSchemaEngine)
            )
            _ <- IO {
              logger.debug(s"foundSchema: ${foundSchema.name}")
            }
          } yield pair
        case Right(other) =>
          logger.warn(s"Unknown value for activeSchemaTab: $other")
          IO((None, Left(s"Unknown value for activeSchemaTab: $other")))
        case Left(msg) =>
          logger.warn(msg)
          IO((None, Left(msg)))
      }

    maybeSchema
  }

  def parseSchemaTab(tab: String): Either[String, SchemaInputType] = {
    val inputTypes = List(SchemaUrlType, SchemaFileType, SchemaTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None =>
        Left(
          s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]"
        )
    }
  }

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

}

object SchemaParam extends LazyLogging {

  private[api] def mkSchema(
      partsMap: PartsMap,
      data: Option[RDFReasoner]
  ): IO[(Schema, SchemaParam)] = {
    val result: IO[Either[String, (Schema, SchemaParam)]] = for {
      sp <- {
        mkSchemaParam(partsMap)
      }
      eitherPair <- sp.getSchema(data).attempt
      resp <- eitherPair.fold(
        err => IO.pure(Left(err.getMessage)),
        pair => {
          val (maybeStr, maybeSchema) = pair
          maybeSchema match {
            // TODO: HERE ERROR IS NULL
            case Left(str) => IO.pure(Left(str))
            case Right(schema) =>
              IO.pure(Right((schema, sp.copy(schema = maybeStr))))
          }
        }
      )
    } yield resp
    result.flatMap(
      _.fold(
        errMsg => {
          logger.error(errMsg)
          IO.raiseError(
            new RuntimeException(s"Could not obtain schema. $errMsg")
          )
        },
        IO.pure
      )
    )
  }

  private[api] def mkSchemaParam(partsMap: PartsMap): IO[SchemaParam] = for {
    schema            <- partsMap.optPartValue(SchemaParameter.name)
    schemaURL         <- partsMap.optPartValue(SchemaURLParameter.name)
    schemaFile        <- partsMap.optPartValue(SchemaFileParameter.name)
    schemaFormatValue <- getSchemaFormat(SchemaFormatParameter.name, partsMap)
    schemaEngine      <- partsMap.optPartValue(SchemaEngineParameter.name)
    targetSchemaEngine <- partsMap.optPartValue(
      TargetSchemaEngineParameter.name
    )
    targetSchemaFormat <- partsMap.optPartValue(
      TargetSchemaFormatParameter.name
    )
    activeSchemaTab <- partsMap.optPartValue(ActiveSchemaTabParameter.name)
  } yield {
    SchemaParam(
      schema,
      schemaURL,
      schemaFile,
      schemaFormatValue,
      schemaEngine,
      targetSchemaEngine,
      targetSchemaFormat,
      activeSchemaTab
    )
  }

  private def getSchemaFormat(
      name: String,
      partsMap: PartsMap
  ): IO[Option[SchemaFormat]] = for {
    maybeStr <- partsMap.optPartValue(name)
  } yield maybeStr match {
    case None => None
    case Some(str) =>
      SchemaFormat
        .fromString(str)
        .fold(
          err => {
            logger.error(s"Unsupported schemaFormat for $name: $str")
            None
          },
          df => Some(df)
        )
  }

  private[api] def empty: SchemaParam =
    SchemaParam(
      schema = None,
      schemaURL = None,
      schemaFile = None,
      optSchemaFormat = None,
      schemaEngine = None,
      targetSchemaEngine = None,
      targetSchemaFormat = None,
      activeSchemaTab = None
    )

}

/** Enumeration of the different possible Schema tabs sent by the client.
  * The tab sent indicates the API if the schema was sent in raw text, as a URL
  * to be fetched or as a text file containing the schema.
  * In case the client submits the schema in several formats, the selected tab will indicate the preferred one.
  */
private[logic] object SchemaTab extends Enumeration {
  type SchemaTab = String

  val TEXT = "#schemaTextArea"
  val URL  = "#schemaUrl"
  val FILE = "#schemaFile"

  val defaultActiveShapeMapTab: SchemaTab = TEXT
}
