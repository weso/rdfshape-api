package es.weso.rdfshape.server.api.routes.schema.logic.types

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource.SchemaSource
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.schema.{Schema => SchemaW}
import io.circe.{Decoder, Encoder, HCursor}

/** Common trait to all schemas, whichever its nature
  */
trait Schema {

  /** Either the raw schema contents represented as a String,
    * or the error occurred when trying to parse the schema
    */
  lazy val rawSchema: Either[String, String] = Left("")

  /** Default URI obtained from current folder
    */
  lazy val base: Option[IRI] = Some(ApiDefaults.localBase)
  // ApiDefaults.relativeBase

  /** Source where the schema comes from
    */
  val schemaSource: SchemaSource

  /** Format of the schema
    */
  val format: Option[SchemaFormat] = None

  /** Engine used for operating the schema
    */
  val engine: Option[SchemaW] = None

  /** Get the inner schema entity of type [[SchemaW]], which is used internally for schema operations
    *
    * @return Either the inner Schema logical model as used by WESO libraries,
    *         or an error extracting the model
    */
  def getSchema: IO[Either[String, SchemaW]]
}

object Schema extends SchemaCompanion[Schema] {

  /** Dummy implementation meant to be overridden
    *
    * @note Resort by default to [[SchemaSimple]]'s empty representation
    */
  override val emptySchema: Schema = SchemaSimple.emptySchema

  /** Dummy implementation meant to be overridden.
    * If called on a general [[Schema]] instance, pattern match among the available data types to
    * use the correct implementation
    */
  override implicit val encodeSchema: Encoder[Schema] = {
    case ss: SchemaSimple => SchemaSimple.encodeSchema(ss)
  }

  /** Dummy implementation meant to be overridden
    * If called on a general [[Schema]] instance, pattern match among the available data types to
    * use the correct implementation
    *
    * @note Defaults to [[SchemaSimple]]'s implementation of decoding data
    */
  override implicit val decodeSchema: Decoder[Schema] = (cursor: HCursor) => {
    this.getClass match {
      case ss if ss == classOf[SchemaSimple] =>
        SchemaSimple.decodeSchema(cursor)
    }
  }

  /** Build a [[Schema]] from request parameters
    *
    * @param partsMap Request parameters
    * @note General implementation delegating on subclasses
    */
  override def mkSchema(partsMap: PartsMap): IO[Either[String, Schema]] = for {
    // 1. Make some checks on the parameters to distinguish between Schema types
    // 2. Delegate on the correct sub-class for creating the Schema
    maybeSchema <- SchemaSimple.mkSchema(partsMap)
  } yield maybeSchema
}

/** Static utilities to be used with [[Schema]] representations
  *
  * @tparam S Specific [[Schema]] representation to be handled
  */
private[schema] trait SchemaCompanion[S <: Schema] extends LazyLogging {

  /** Empty instance of the [[Schema]] representation in use
    */
  val emptySchema: S

  /** Encoder used to transform [[Schema]] instances to JSON values
    */
  implicit val encodeSchema: Encoder[S]

  /** Decoder used to extract [[Schema]] instances from JSON values
    */
  implicit val decodeSchema: Decoder[S]

  /** Given a request's parameters, try to extract an instance of [[Schema]] (type [[S]]) from them
    *
    * @param partsMap Request's parameters
    * @return Either the [[Schema]] instance or an error message
    */
  def mkSchema(partsMap: PartsMap): IO[Either[String, S]]
}
