package es.weso.rdfshape.server.api.routes.schema.logic.types

import cats.effect._
import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.{
  SchemaFormat,
  ShaclFormat
}
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

/** Case class representing a single Schema instance with its inner content, format, engine and source
  *
  * @param schemaPre    Schema data, as it is received before being processed depending on the [[schemaSource]]
  * @param schemaFormat Schema format
  * @param schemaEngine Schema engine of type [[SchemaW]], used to know how to internally process schema operations
  * @param schemaSource Origin source, used to know how to process the raw data
  */
sealed case class SchemaSimple(
    private val schemaPre: Option[String],
    private val schemaFormat: SchemaFormat,
    private val schemaEngine: SchemaW,
    override val schemaSource: SchemaSource
) extends Schema
    with LazyLogging {

  /** Given the user input ([[schemaPre]]) for the schema and its source, fetch the Schema contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Either an error creating the raw data or a String containing the final schema text
    */
  override lazy val rawSchema: Either[String, String] = schemaPre match {
    case None => Left("Could not build the Schema from empty data")
    case Some(userSchema) =>
      schemaSource match {
        case SchemaSource.TEXT | SchemaSource.FILE => Right(userSchema)
        case SchemaSource.URL =>
          getUrlContents(userSchema)

        case other =>
          val msg = s"Unknown schema source: $other"
          logger.warn(msg)
          Left(msg)

      }
  }
  // Override and make publicly available the trait properties
  override val format: Option[SchemaFormat] = Option(schemaFormat)
  override val engine: Option[SchemaW]      = Option(schemaEngine)

  def getPrefixMap: IO[Option[PrefixMap]] = for {
    model <- getSchema
  } yield model.map(_.pm).toOption

  override def getSchema: IO[Either[String, SchemaW]] = {
    rawSchema match {
      case Right(schemaStr) =>
        for {
          schemaW <- Schemas
            .fromString(
              str = schemaStr,
              format = schemaFormat.name,
              schemaEngine.name,
              base.map(_.str)
            )
            .attempt // Catch unexpected exceptions early

        } yield schemaW.leftMap(err =>
          Option(err.getMessage).getOrElse(
            "Unknown error processing the schema"
          )
        )
      // No schema data, propagate the error
      case Left(err) => IO.pure(Left(err))
    }
  }

}

private[api] object SchemaSimple
    extends SchemaCompanion[SchemaSimple]
    with LazyLogging {

  /** Empty schema representation, with no inner data and all defaults or None
    */
  override lazy val emptySchema: SchemaSimple =
    SchemaSimple(
      schemaPre = None,
      schemaFormat = ApiDefaults.defaultSchemaFormat,
      schemaEngine = ApiDefaults.defaultSchemaEngine,
      schemaSource = ApiDefaults.defaultSchemaSource
    )

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
      schemaSource = paramSource.getOrElse(SchemaSource.defaultSchemaSource)
      _ = logger.debug(
        s"Schema received ($schemaFormat) - Source: $schemaSource"
      )

      // Base for the result
      schema = SchemaSimple(
        schemaPre = paramSchema,
        schemaFormat = schemaFormat,
        schemaEngine = schemaEngine,
        schemaSource = schemaSource
      )

    } yield schema.rawSchema.fold(
      err => Left(err),
      _ => Right(schema)
    )

  override implicit val encodeSchema: Encoder[SchemaSimple] =
    (schema: SchemaSimple) => {
      Json.obj(
        ("schema", schema.rawSchema.toOption.asJson),
        ("format", schema.schemaFormat.asJson),
        ("engine", schema.schemaEngine.asJson),
        ("source", schema.schemaSource.asJson)
      )
    }
  override implicit val decodeSchema: Decoder[SchemaSimple] =
    (cursor: HCursor) =>
      for {
        schema <- cursor.downField("schema").as[Option[String]]

        schemaFormat <- cursor
          .downField("schemaFormat")
          .as[ShaclFormat]

        schemaEngine <-
          cursor
            .downField("schemaEngine")
            .as[SchemaW]

        schemaSource <- cursor
          .downField("schemaSource")
          .as[SchemaSource]
          .orElse(Right(SchemaSource.defaultSchemaSource))

        decoded = SchemaSimple.emptySchema.copy(
          schemaPre = schema,
          schemaFormat,
          schemaEngine,
          schemaSource
        )

      } yield decoded
}
