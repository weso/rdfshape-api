package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType.TriggerModeType
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{ShapeMapTrigger, ValidationTrigger}
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe._
import io.circe.syntax.EncoderOps

import scala.language.implicitConversions
import scala.util.Try

/** Data class representing a validation trigger enabled by a shapemap,
  * for ShEx validations.
  *
  * @param shapeMap Inner shapemap associated to the [[TriggerShapeMap]]
  */
sealed case class TriggerShapeMap(
    shapeMap: ShapeMap,
    override val data: Option[Data] = None,
    override val schema: Option[Schema] = None
) extends TriggerMode
    with LazyLogging {

  /** Inner shapemap structure of the shapemap contained in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = shapeMap.innerShapeMap
  override val _type: TriggerModeType               = TriggerModeType.SHAPEMAP

  // Fetched shapeMap successfully
  assume(
    innerShapeMap.isRight,
    innerShapeMap.left.getOrElse("Unknown error creating the TriggerMode")
  )
  val shapeMapW: ShapeMapW = innerShapeMap.toOption.get

  override def getValidationTrigger: ValidationTrigger = {
    ShapeMapTrigger(shapeMapW)
  }
}

private[api] object TriggerShapeMap
    extends TriggerModeCompanion[TriggerShapeMap]
    with LazyLogging {

  override implicit def decode(
      data: Option[Data],
      schema: Option[Schema]
  ): Decoder[Either[String, TriggerShapeMap]] =
    (cursor: HCursor) => {
      for {
        maybeShapeMap <- cursor
          .as[Either[String, ShapeMap]]

        // Get the prefix maps from data and schema, the shapeMap needs them
        // as reference
        prefixMaps <- Try {
          val mapsIo = for {
            // Get data prefix map, if possible
            dataPrefixMap <- data match {
              case Some(data) =>
                data.toRdf().map(_.use(_.getPrefixMap)).flatten
              case None => IO.pure(PrefixMap.empty)
            }
            // Get schema prefix map, if possible
            schemaPrefixMap <- schema match {
              case Some(schema) =>
                schema.getSchema.map(
                  _.map(_.pm).toOption.getOrElse(PrefixMap.empty)
                )
              case None => IO.pure(PrefixMap.empty)
            }
          } yield (dataPrefixMap, schemaPrefixMap)
          mapsIo.unsafeRunSync()
        }.toEither.leftMap(ex =>
          DecodingFailure(
            s"Could not process the user data provided:\n${ex.getMessage}",
            Nil
          )
        )
      } yield maybeShapeMap.flatMap(sm =>
        /* Finally, try to build the object, catching any exception */
        Try {
          val finalShapeMap = sm.copy(
            nodesPrefixMap = prefixMaps._1,
            shapesPrefixMap = prefixMaps._2
          )
          TriggerShapeMap(finalShapeMap, data, schema)
        }.toEither
          .leftMap(err =>
            s"Could not build the shapeMap trigger-mode from user data:\n ${err.getMessage}"
          )
      )
    }

  override implicit val encode: Encoder[TriggerShapeMap] =
    (tsm: TriggerShapeMap) =>
      Json.obj(
        ("type", tsm._type.asJson),
        ("shapeMap", tsm.shapeMap.asJson),
        ("data", tsm.data.asJson),
        ("schema", tsm.schema.asJson)
      )

  /** Given a request's parameters, try to extract a TriggerMode instance from them
    *
    * @param partsMap Request's parameters
    * @return Either the trigger mode or an error message
    */
  def mkTriggerMode(
      partsMap: PartsMap,
      data: Option[Data],
      schema: Option[Schema]
  ): IO[Either[String, TriggerShapeMap]] = {
    // Get data prefix map, if possible
    val dataPrefixMap: Option[PrefixMap] = data
      .map(data =>
        for {
          rdf <- data.toRdf()
          pm  <- rdf.use(_.getPrefixMap)
        } yield pm
      )
      .map(
        _.handleErrorWith(_ =>
          IO.raiseError(
            new RuntimeException("Could not process the data provided")
          )
        )
          .unsafeRunSync()
      )

    // Get schema prefix map, if possible
    val schemaPrefixMap: Option[PrefixMap] = schema
      .map(schema =>
        for {
          schemaModel <- schema.getSchema
          pm = schemaModel.map(_.pm).toOption
        } yield pm
      )
      .flatMap(
        _.handleErrorWith(_ =>
          IO.raiseError(
            new RuntimeException("Could not process the schema provided")
          )
        ).unsafeRunSync()
      )

    // Form the shapemap and complete the trigger instance
    for {
      // Get companion shapemap from params
      maybeShapeMap <- ShapeMap.mkShapeMap(
        partsMap,
        nodesPrefixMap = dataPrefixMap,
        shapesPrefixMap = schemaPrefixMap
      )

      // Create TriggerMode instance
      maybeTriggerMode = maybeShapeMap.map(shapeMap =>
        TriggerShapeMap(shapeMap, data, schema)
      )

    } yield maybeTriggerMode
  }
}
