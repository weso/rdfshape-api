package es.weso.rdfshape.server.api.routes.schema.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.types.{
  Schema,
  SchemaSimple
}
import es.weso.schema.{ShExSchema, ShaclexSchema, Schema => SchemaW}
import es.weso.shacl.converter.Shacl2ShEx
import io.circe.syntax._
import io.circe.{Encoder, Json}

/** Data class representing the output of a schema-conversion operation
  *
  * @param inputSchema Schema used as input of the operation
  * @param result      [[Schema]] resulting from the conversion
  */
final case class SchemaConvert private (
    override val inputSchema: Schema,
    targetFormat: Option[String],
    targetEngine: Option[String],
    result: Schema
) extends SchemaOperation(SchemaConvert.successMessage, inputSchema)

private[api] object SchemaConvert extends LazyLogging {

  private val successMessage = "Conversion successful"

  /** JSON encoder for [[SchemaConvert]]
    */
  implicit val encodeSchemaConvertOperation: Encoder[SchemaConvert] =
    (schemaConvert: SchemaConvert) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(schemaConvert.successMessage)),
          ("schema", schemaConvert.inputSchema.asJson),
          ("result", schemaConvert.result.asJson)
        )
      )

  /** Perform the actual conversion operation between Schema formats
    *
    * @param schema       Input conversion schema
    * @param targetFormat Target format
    * @param targetEngine Target engine
    * @return A new [[Schema]] instance
    */
  def schemaConvert(
      schema: Schema,
      targetFormat: SchemaFormat,
      targetEngine: Option[SchemaW]
  ): IO[Schema] = {
    logger.info(
      s"Schema conversion target format/engine: ${targetFormat.name}/${targetEngine
        .map(_.name)}"
    )

    // Check the schema engine
    schema.engine match {
      case Some(engine) =>
        engine match {
          // Test that we are using shaclex schemas, specifically ShEx,
          // which implements conversion
          case shaclex: ShaclexSchema
              if shaclex.getClass == classOf[ShExSchema] =>
            logger.debug("Schema conversion: SHACLEX -> SHEX")
            Shacl2ShEx.shacl2ShEx(
              schema = shaclex.schema,
              nodesPrefixMap = Option(shaclex.pm)
            ) match {
              case Left(err) =>
                val msg = s"Error converting schema: $err"
                logger.error(msg)
                IO.raiseError(new RuntimeException(msg))
              case Right(newSchema) =>
                // ShapeMap generated here as well, but unneeded
                val (schema, _) = newSchema
                for {
                  emptySchemaBuilder <- RDFAsJenaModel.empty
                  rawString <- emptySchemaBuilder.use(builder =>
                    es.weso.shex.Schema.serialize(
                      schema,
                      targetFormat.name,
                      None,
                      builder
                    )
                  )
                } yield SchemaSimple(
                  schemaPre = Option(rawString),
                  schemaFormat = targetFormat,
                  schemaEngine = engine,
                  schemaSource = SchemaSource.TEXT
                )
            }

          case _ =>
            IO.raiseError(
              new RuntimeException(
                "Could not perform conversion, unknown input schema engine"
              )
            )
        }
      case None =>
        IO.raiseError(
          new RuntimeException(
            "Could not perform conversion, unspecified input schema engine"
          )
        )
    }
  }
}
