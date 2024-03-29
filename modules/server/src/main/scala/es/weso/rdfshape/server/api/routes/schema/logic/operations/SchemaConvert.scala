package es.weso.rdfshape.server.api.routes.schema.logic.operations

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.definitions.UmlDefinitions.umlOptions
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  GraphicFormat,
  Svg,
  Json => JsonFormat
}
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.types.{
  Schema,
  SchemaSimple
}
import es.weso.schema.{
  JenaShacl,
  ShExSchema,
  ShaclTQ,
  ShaclexSchema,
  Schema => SchemaW
}
import es.weso.shacl.converter.Shacl2ShEx
import es.weso.uml.{Schema2UML, UML}
import io.circe.syntax._
import io.circe.{Encoder, Json}

/** Data class representing the output of a schema-conversion operation
  *
  * @param inputSchema Schema used as input of the operation
  * @param result      [[Schema]] resulting from the conversion
  */
final case class SchemaConvert private (
    override val inputSchema: Schema,
    targetFormat: SchemaFormat,
    targetEngine: SchemaW,
    result: Schema
) extends SchemaOperation(SchemaConvert.successMessage, inputSchema)

private[api] object SchemaConvert extends LazyLogging {

  /** JSON encoder for [[SchemaConvert]]
    */
  implicit val encodeSchemaConvertOperation: Encoder[SchemaConvert] =
    (schemaConvert: SchemaConvert) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(schemaConvert.successMessage)),
          ("schema", schemaConvert.inputSchema.asJson),
          ("result", schemaConvert.result.asJson),
          ("targetSchemaFormat", schemaConvert.targetFormat.asJson)
        )
      )
  private val successMessage = "Conversion successful"

  /** Perform the actual conversion operation between Schema formats.
    *
    * @note Firstly, check the conversion target
    *       (another Schema, a Graphic visualization, etc.).
    *       Secondly, invoke the corresponding logic for each transformation
    * @param inputSchema  Input conversion schema
    * @param targetFormat Target format
    * @param targetEngine Target engine (discarded in visualizations)
    * @return A new [[SchemaConvert]] instance with the conversion information
    */
  def schemaConvert(
      inputSchema: Schema,
      targetFormat: DataFormat,
      targetEngine: SchemaW
  ): IO[SchemaConvert] = {
    logger.debug(
      s"""Schema conversion targets:
          - Format: ${targetFormat.name}
          - Engine: ${targetEngine.name}"""
    )

    // Check the format nature to see which logic to invoke
    // 1) Schema to Schema conversion
    if(SchemaFormat.availableFormats.contains(targetFormat)) {
      schemaToSchema(
        inputSchema,
        new SchemaFormat(targetFormat),
        targetEngine
      )
    }
    // 2) Schema to visualization
    else if(GraphicFormat.availableFormats.contains(targetFormat)) {
      schemaToVisualization(inputSchema, new GraphicFormat(targetFormat))
    }
    // 3) Other conversions available
    else
      targetFormat match {
        case JsonFormat =>
          schemaToJson(inputSchema)
        case _ =>
          val msg =
            s"Unavailable conversion from ${inputSchema.format.name} to ${targetFormat.name}"
          logger.error(msg)
          IO.raiseError(new RuntimeException(msg))
      }

  }

  /** *
    * Auxiliary method for [[schemaConvert]]
    * Convert a given Schema to another one, given the target engine and format
    *
    * @param inputSchema  Input schema for conversion
    * @param targetFormat Target format
    * @param targetEngine Target engine
    * @return A new [[SchemaConvert]] instance with the conversion information
    */
  private def schemaToSchema(
      inputSchema: Schema,
      targetFormat: SchemaFormat,
      targetEngine: SchemaW
  ): IO[SchemaConvert] = {
    // Try to extract the inner-library schema from the user given schema
    for {
      maybeInnerSchema <- inputSchema.getSchema
      innerSchema <- maybeInnerSchema match {
        case Left(err)     => IO.raiseError(new RuntimeException(err))
        case Right(schema) => IO.pure(schema)
      }

      // Tuple with the input data and an empty representation of the output
      conversionSchemas = (
        innerSchema,
        targetEngine
      )
      _ = logger.debug(
        s"Schema conversion: ${conversionSchemas._1.name} -> ${conversionSchemas._2.name}"
      )

      result <- conversionSchemas match {
        // If SHEX => SHEX or SHACL => SHACL, use the schema methods
        case (schemaIn: SchemaW, schemaOut: SchemaW)
            if schemaIn.getClass == schemaOut.getClass =>
          for {
            rawOutputSchema <- schemaIn
              .convert(
                Option(targetFormat.name),
                Option(schemaOut.name),
                None
              )
            outputSchema = SchemaSimple(
              content = rawOutputSchema.trim,
              format = targetFormat,
              engine = schemaOut,
              source = SchemaSource.TEXT
            )
          } yield SchemaConvert(
            inputSchema = inputSchema,
            targetFormat = targetFormat,
            targetEngine = targetEngine,
            result = outputSchema
          )

        // SHACL => SHEX, use implemented
        case (shaclIn: ShaclexSchema, shexOut: ShExSchema) =>
          logger.debug("Schema conversion: SHACL(EX) -> SHEX")
          for {
            shexSchema <- shaclToShex(shaclIn, targetFormat)
            outputSchema = shexSchema match {
              case (_, schemaStr) =>
                SchemaSimple(
                  content = schemaStr.trim,
                  format = targetFormat,
                  engine = shexOut,
                  source = SchemaSource.TEXT
                )
            }
          } yield SchemaConvert(
            inputSchema = inputSchema,
            targetFormat = targetFormat,
            targetEngine = outputSchema.engine,
            result = outputSchema
          )
        // SHEX => SHACL, not implemented
        case (_: ShExSchema, ShaclexSchema(_) | JenaShacl(_) | ShaclTQ(_)) =>
          IO.raiseError(
            new RuntimeException("Not implemented: ShEx to SHACL")
          )
        case (a, b) =>
          logger.error(s"${a.getClass.getName}, ${b.getClass.getName}")
          IO.raiseError(
            new RuntimeException(
              "Could not perform conversion: invalid schema inputs"
            )
          )
      }
    } yield result
  }

  /** Auxiliary method with the logic to convert from SHACL schemas to SHEX schemas
    *
    * @param inputSchema [[ShaclexSchema]] used for the conversion
    * @return [[ShExSchema]] resulting of the conversion, along with the raw schema String
    */
  private def shaclToShex(
      inputSchema: ShaclexSchema,
      targetFormat: SchemaFormat
  ): IO[(ShExSchema, String)] = {
    Shacl2ShEx.shacl2ShEx(
      schema = inputSchema.schema,
      nodesPrefixMap = Option(inputSchema.pm)
    ) match {
      case Left(err) =>
        val msg = s"Error converting schema: $err"
        logger.error(msg)
        IO.raiseError(new RuntimeException(msg))
      case Right(newSchema) =>
        // ShapeMap generated here as well, but unneeded
        val (schemaW, _) = newSchema
        for {
          emptySchemaBuilder <- RDFAsJenaModel.empty
          targetFormatStr = targetFormat.name
          rawString <- emptySchemaBuilder.use(builder =>
            es.weso.shex.Schema.serialize(
              schemaW,
              targetFormatStr,
              None,
              builder
            )
          )
          result <- ShExSchema.fromString(rawString, targetFormatStr, None)
        } yield (result, rawString)
    }
  }

  /** *
    * Auxiliary method for [[schemaConvert]]
    * Convert a given Schema to its JSON representation, for latter use
    * individually or with cytoscape
    *
    * @param inputSchema Input schema for conversion
    * @return A new [[SchemaConvert]] instance with the conversion information
    */
  private def schemaToJson(inputSchema: Schema): IO[SchemaConvert] =
    for {
      innerSchema <- inputSchema.getSchema
      jsonSchema = innerSchema
        .flatMap(s => Schema2UML.schema2UML(s))
        .map(_._1.toJson)

      outputSchema = jsonSchema.map(jsonData =>
        SchemaSimple(
          content = jsonData.spaces2,
          format = new SchemaFormat(JsonFormat),
          engine = inputSchema.engine,
          source = SchemaSource.TEXT
        )
      )

      conversion <- outputSchema.fold(
        err => IO.raiseError(new RuntimeException(err)),
        schema =>
          IO.pure(
            SchemaConvert(
              inputSchema = inputSchema,
              targetFormat = new SchemaFormat(JsonFormat),
              targetEngine = inputSchema.engine,
              result = schema
            )
          )
      )
    } yield conversion

  /** *
    * Auxiliary method for [[schemaConvert]]
    * Convert a given Schema to its SVG representation, for latter use
    * in clients
    *
    * @param inputSchema Input schema for conversion
    * @return A new [[SchemaConvert]] instance with the conversion information
    */
  private def schemaToVisualization(
      inputSchema: Schema,
      targetFormat: GraphicFormat
  ): IO[SchemaConvert] = for {

    maybeInnerSchema <- inputSchema.getSchema
    maybeUml: Either[String, (UML, List[String])] = maybeInnerSchema.flatMap(
      s => Schema2UML.schema2UML(s)
    )
    maybeResult: Either[String, IO[Schema]] = maybeUml.flatMap {
      case (uml, _) =>
        targetFormat match {
          case Svg =>
            Right(
              uml
                .toSVG(umlOptions)
                .map(svg => {
                  SchemaSimple(
                    content = svg.trim,
                    format = new SchemaFormat(Svg),
                    engine = inputSchema.engine,
                    source = SchemaSource.TEXT
                  )
                })
            )

          case _ => Left(s"Unsupported visualization: ${targetFormat.name}")
        }
    }

    conversion <- maybeResult.fold(
      err => IO.raiseError(new RuntimeException(err)),
      _.map(resultSchema =>
        SchemaConvert(
          inputSchema = inputSchema,
          targetFormat = resultSchema.format,
          targetEngine = inputSchema.engine,
          result = resultSchema
        )
      ).handleErrorWith(err =>
        IO.raiseError(
          new RuntimeException(
            s"Unexpected error during conversion: ${err.getMessage}"
          )
        )
      )
    )

  } yield conversion
}
