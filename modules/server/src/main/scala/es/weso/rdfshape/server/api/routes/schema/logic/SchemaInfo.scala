package es.weso.rdfshape.server.api.routes.schema.logic

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

/** Data class representing the data contained in a schema
  *
  * @param schemaType      Name of the schema
  * @param schemaEngine    Engine of the schema
  * @param wellFormed      Is the schema well formed
  * @param shapes          List of shapes in the schema
  * @param shapesPrefixMap Prefix map of the shapes in the schema
  * @param errors          Errors in the schema
  */
private[schema] case class SchemaInfo(
    schemaType: Option[String],
    schemaEngine: Option[String],
    wellFormed: Boolean,
    shapes: List[String],
    shapesPrefixMap: List[(String, String)],
    errors: List[String]
) {

  /** Transform a Schema Info result to a JSON representation
    *
    * @return JSON representation of the schema information
    */
  def toJson: Json = Json.fromFields(
    List(
      ("schemaType", schemaType.fold(Json.Null)(Json.fromString)),
      ("schemaEngine", schemaEngine.fold(Json.Null)(Json.fromString)),
      ("wellFormed", Json.fromBoolean(wellFormed)),
      ("shapes", Json.fromValues(shapes.map(Json.fromString))),
      (
        "shapesPrefixMap",
        Json.fromValues(
          shapesPrefixMap.map(pair =>
            Json.fromFields(
              List(
                ("prefix", Json.fromString(pair._1)),
                ("uri", Json.fromString(pair._2))
              )
            )
          )
        )
      ),
      ("error", Json.fromValues(errors.map(Json.fromString)))
    )
  )
}

/** Static utilities of the SchemaInfoReply class
  */
object SchemaInfo extends LazyLogging {

  /** Create an empty SchemaInfoReply with an error message.
    * Used when errors occur extracting the schema information
    *
    * @param msg Message attached to the failing schema
    * @return Empty SchemaInfoReply object with no data except for an error message
    */
  def fromError(msg: String): SchemaInfo = {
    logger.debug(s"SchemaInfoReply from $msg")
    SchemaInfo(None, None, wellFormed = false, List(), List(), List(msg))
  }
}
