package es.weso.rdfshape.server.api.routes.schema.logic.trigger

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerModeType.TriggerModeType
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMap
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{ShapeMapTrigger, ValidationTrigger}
import es.weso.shapemaps.{ShapeMap => ShapeMapW}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Data class representing a validation trigger enabled by a shapemap,
  * for ShEx validations.
  *
  * @param shapeMap Inner shapemap associated to the [[TriggerShapeMap()]]
  */
sealed case class TriggerShapeMap(
    shapeMap: ShapeMap,
    override val data: Option[Data],
    override val schema: Option[Schema]
) extends TriggerMode
    with LazyLogging {

  /** Inner shapemap structure of the shapemap contained in this instance
    *
    * @return A ShapeMap instance used by WESO libraries in validation
    */
  lazy val innerShapeMap: Either[String, ShapeMapW] = shapeMap.innerShapeMap

  override val triggerModeType: TriggerModeType = TriggerModeType.SHAPEMAP

  override def getValidationTrigger: Either[String, ValidationTrigger] =
    innerShapeMap.map(ShapeMapTrigger(_))
}

private[api] object TriggerShapeMap
    extends TriggerModeCompanion[TriggerShapeMap]
    with LazyLogging {

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

  override implicit val encodeTriggerMode: Encoder[TriggerShapeMap] =
    (tsm: TriggerShapeMap) =>
      Json.obj(
        ("type", tsm.triggerModeType.asJson),
        ("shapeMap", tsm.shapeMap.asJson),
        ("data", tsm.data.asJson),
        ("schema", tsm.schema.asJson)
      )

  override implicit val decodeTriggerMode: Decoder[TriggerShapeMap] =
    (cursor: HCursor) =>
      for {
        shapeMap <- cursor.downField("shapeMap").as[ShapeMap]
        data     <- cursor.downField("data").as[Option[Data]]
        schema   <- cursor.downField("schema").as[Option[Schema]]
        decoded = TriggerShapeMap(shapeMap, data, schema)
      } yield decoded
}
