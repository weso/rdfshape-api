package es.weso.rdfshape.server.api.routes.schema.logic.types

import cats.effect._
import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.aux.SchemaAdapter
import es.weso.rdfshape.server.api.routes.schema.logic.aux.SchemaAdapter._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import es.weso.schema.{Schemas, Schema => SchemaW}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.util.Try

/** Case class representing a single Schema instance with its inner content, format, engine and source
  *
  * @param content Schema data, as it is received before being processed depending on the [[source]]
  * @param format  Schema format
  * @param engine  Schema engine of type [[SchemaW]], used to know how to internally process schema operations
  * @param source  Origin source, used to know how to process the raw data
  */
sealed case class SchemaSimple(
    private val content: String,
    override val format: SchemaFormat = SchemaFormat.default,
    override val engine: SchemaW,
    override val source: SchemaSource = SchemaSource.default
) extends Schema
    with LazyLogging {

  // Non empty content
  assume(!content.isBlank, "Could not build the schema from empty data")
  // Valid source
  assume(
    SchemaSource.values.exists(_ equalsIgnoreCase source),
    s"Unknown schema source: '$source'"
  )

  override lazy val fetchedContents: Either[String, String] =
    if(source equalsIgnoreCase SchemaSource.URL)
      getUrlContents(content)
    // Text or file
    else Right(content)

  // Fetched contents successfully
  assume(
    fetchedContents.isRight,
    fetchedContents.left.getOrElse("Unknown error")
  )

  // Get prefix map when needed
  lazy val prefixMap: IO[Option[PrefixMap]] = for {
    model <- getSchema
  } yield model.map(_.pm).toOption

  override val raw: String = fetchedContents.toOption.get

  override def getSchema: IO[Either[String, SchemaW]] =
    for {
      schemaW <- Schemas
        .fromString(
          str = raw,
          format = format.name,
          engine.name,
          base.map(_.str)
        )
        .attempt // Catch unexpected exceptions early

    } yield schemaW.leftMap(err =>
      Option(err.getMessage).getOrElse(
        "Unknown error processing the schema"
      )
    )

}

private[api] object SchemaSimple
    extends SchemaCompanion[SchemaSimple]
    with LazyLogging {

  override implicit val encodeSchema: Encoder[SchemaSimple] =
    (schema: SchemaSimple) => {
      Json.obj(
        ("content", schema.raw.asJson),
        ("format", schema.format.asJson),
        ("engine", schema.engine.asJson),
        ("source", schema.source.asJson)
      )
    }
  override implicit val decodeSchema: Decoder[Either[String, SchemaSimple]] =
    (cursor: HCursor) => {
      val schemaData = for {
        content <- cursor
          .downField(ContentParameter.name)
          .as[String]
          .map(_.trim)

        maybeFormat <- cursor
          .downField(FormatParameter.name)
          .as[Either[String, SchemaFormat]]

        maybeEngine <-
          cursor
            .downField(EngineParameter.name)
            .as[Either[String, SchemaW]]

        source <- cursor
          .downField(SourceParameter.name)
          .as[SchemaSource]

      } yield (content, maybeFormat, maybeEngine, source)

      schemaData.map {
        /* Destructure and try to build the object, catch the exception as error
         * message if needed */
        case (content, maybeFormat, maybeEngine, source) =>
          for {
            format <- maybeFormat
            engine <- maybeEngine
            schema <- Try {
              SchemaSimple(content, format, engine, source)
            }.toEither.leftMap(err =>
              s"Could not build the schema from user data:\n ${err.getMessage}"
            )
          } yield schema
      }
    }

  override def mkSchema(partsMap: PartsMap): IO[Either[String, SchemaSimple]] =
    for {
      // Schema param as sent by client
      paramSchema <- partsMap.optPartValue(SchemaParameter.name)
      paramFormat <- SchemaFormat.fromRequestParams(
        SchemaFormatParameter.name,
        partsMap
      )
      paramEngine <- partsMap.optPartValue(SchemaEngineParameter.name)
      paramSource <- partsMap.optPartValue(SchemaSourceParameter.name)
      _ = Schemas.availableSchemaNames
      // Confirm format and engine or resort to defaults
      schemaFormat = paramFormat.getOrElse(ApiDefaults.defaultSchemaFormat)
      schemaEngine = paramEngine
        .flatMap(SchemaAdapter.schemaEngineFromString)
        .getOrElse(ApiDefaults.defaultSchemaEngine)

      // Check the client's selected source
      schemaSource = paramSource.getOrElse(SchemaSource.default)
      _ = logger.debug(
        s"Schema received ($schemaFormat) - Source: $schemaSource"
      )

      // Base for the result
      schema = SchemaSimple(
        content = "",
        format = schemaFormat,
        engine = schemaEngine,
        source = schemaSource
      )

    } yield schema.fetchedContents.fold(
      err => Left(err),
      _ => Right(schema)
    )
}
