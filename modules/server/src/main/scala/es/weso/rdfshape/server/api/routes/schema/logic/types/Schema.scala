package es.weso.rdfshape.server.api.routes.schema.logic.types

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource.SchemaSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.SourceParameter
import es.weso.schema.{Schema => SchemaW}
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

/** Common trait to all schemas, whichever its nature
  */
trait Schema {

  /** Default URI obtained from current folder
    */
  lazy val base: Option[IRI] = Some(ApiDefinitions.localBase)

  /** Given the user input for the schema and its source, fetch the Schema contents
    * using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files,
    * decode the input; for raw data, do nothing)
    *
    * @return Either the raw schema contents represented as a String,
    *         or the error occurred when trying to parse the schema
    */
  val fetchedContents: Either[String, String]

  /** Raw schema value, i.e.: the text forming the schema
    *
    * @note It is safely extracted from [[fetchedContents]] after asserting
    *       the [[source]] and fetched contents are right
    */
  val raw: String

  /** Source where the schema comes from
    */
  val source: SchemaSource

  /** Format of the schema
    */
  val format: SchemaFormat

  /** Engine used for operating the schema
    */
  val engine: SchemaW

  /** Get the inner schema entity of type [[SchemaW]], which is used internally for schema operations
    *
    * @return Either the inner Schema logical model as used by WESO libraries,
    *         or an error extracting the model
    */
  def getSchema: IO[Either[String, SchemaW]]

  /** @return A String with the raw contents or the error that made them
    *          un-parseable
    */
  override def toString: String =
    fetchedContents.fold(identity, identity)
}

object Schema extends SchemaCompanion[Schema] {

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
  override implicit val decodeSchema: Decoder[Either[String, Schema]] =
    (cursor: HCursor) => {
      for {
        source <- cursor
          .downField(SourceParameter.name)
          .as[SchemaSource]
          .map(_.toLowerCase)

        decoded <-
          if(
            SchemaSource.values
              .map(_.toLowerCase)
              .contains(source.toLowerCase)
          ) SchemaSimple.decodeSchema(cursor)
          else
            DecodingFailure(
              s"Invalid schema source '$source': use one of '${SchemaSource.values
                .mkString(", ")}'",
              Nil
            ).asLeft

      } yield decoded

    }
}

/** Static utilities to be used with [[Schema]] representations
  *
  * @tparam S Specific [[Schema]] representation to be handled
  */
private[schema] trait SchemaCompanion[S <: Schema] extends LazyLogging {

  /** Encoder used to transform [[Schema]] instances to JSON values
    */
  implicit val encodeSchema: Encoder[S]

  /** Decoder used to extract [[Schema]] instances from JSON values
    * The decoder returns either the decoded Schema or the error occurred
    * in the decoding process
    */
  implicit val decodeSchema: Decoder[Either[String, S]]
}
