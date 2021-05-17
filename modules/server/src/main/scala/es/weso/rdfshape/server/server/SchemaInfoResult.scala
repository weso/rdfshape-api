package es.weso.rdfshape.server.server/*
    val validateOptions: IO[(String, Schema, ValidationTrigger,Result)] = getRDFReader(opts, baseFolder).use(rdf => for {
      schema <- getSchema(opts, baseFolder, rdf)
      triggerName = opts.trigger.toOption.getOrElse(ValidationTrigger.default.name)
      shapeMapStr <- getShapeMapStr(opts)
      trigger <- either2es(ValidationTrigger.findTrigger(triggerName, shapeMapStr, base,
        opts.node.toOption, opts.shapeLabel.toOption,
        rdf.getPrefixMap(), schema.pm))
      outDataFormat = opts.outDataFormat.getOrElse(opts.dataFormat())
      str <- rdf.serialize(outDataFormat)
    } yield (str, schema, trigger))

    validateOptions.attempt.unsafeRunSync match {
      case Left(e) => {
        println(s"Error: $e")
      }
      case Right((str, schema, trigger)) => {
        if (opts.showData()) {
          println(str)
        }
        if (opts.showSchema()) {
          // If not specified uses the input schema format
          val outSchemaFormat = opts.outSchemaFormat.getOrElse(opts.schemaFormat())
          schema.serialize(outSchemaFormat).attempt.unsafeRunSync match {
            case Right(str) => println(str)
            case Left(e) => println(s"Error showing schema $schema with format $outSchemaFormat: ${e.getMessage}")
          }
        }

        if (opts.showShapeMap()) {
          println(s"Trigger shapemap: ${trigger.shapeMap}")
          println(s"ShapeMap: ${trigger.shapeMap.serialize(opts.outShapeMapFormat())}")
          println(s"Trigger json: ${trigger.toJson.spaces2}")
        }

        val result = schema.validate(rdf, trigger)

        if (opts.showLog()) {
          logger.info("Show log info = true")
          logger.info(s"JSON result: ${result.unsafeRunSync().toJsonString2spaces}")
        }

        if (opts.showResult() || opts.outputFile.isDefined) {
          val resultSerialized = result.unsafeRunSync().serialize(opts.resultFormat())
          if (opts.showResult()) println(resultSerialized)
          if (opts.outputFile.isDefined)
            FileUtils.writeFile(opts.outputFile(), resultSerialized)
        }

        if (opts.showValidationReport()) {
          val vr = result.unsafeRunSync().validationReport
          for {
            rdf <- vr
            str = rdf.serialize(opts.validationReportFormat()).unsafeRunSync()
          } yield str

            /*.fold(
            e => println(s"Error: ${e.getMessage}"),
            println(_)
          )*/
        }

        if (opts.cnvEngine.isDefined) {
          logger.error("Conversion between engines don't implemented yet")
        }

        if (opts.time()) {
          val endTime = System.nanoTime()
          val time: Long = endTime - startTime
          printTime("Time", opts, time)
        }

      }
    } */

import io.circe.Json

case class SchemaInfoResult(
    schema: String,
    schemaFormat: String,
    schemaEngine: String,
    shapes: Json,
    prefixMap: Json
)
