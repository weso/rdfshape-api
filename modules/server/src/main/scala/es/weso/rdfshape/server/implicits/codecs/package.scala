package es.weso.rdfshape.server.implicits

import cats.implicits.toBifunctorOps
import es.weso.rdf.nodes.IRI
import io.circe._
import org.http4s.Uri

import java.net.URL
import scala.util.Try

/** Additional codecs used throughout the application
  */
package object codecs {

  /** JSON encoder for the [[Uri]] class used in http4s
    */
  implicit val encodeUri: Encoder[Uri] = (uri: Uri) =>
    Json.fromString(uri.renderString)

  /** JSON decoder for [[URL]]. Returns an option since the URL may be malformed
    * and thus None is decoded
    */
  implicit val decodeUrl: Decoder[Either[String, URL]] = (cursor: HCursor) =>
    for {
      urlStr <- cursor.value.as[String]
    } yield Try {
      new URL(urlStr)
    }.toEither.leftMap(_.getMessage)

  /** JSON decoder for [[URL]]. Returns an option since the URL may be malformed
    * and thus None is decoded
    */
  implicit val decodeUri: Decoder[Either[String, Uri]] = (cursor: HCursor) => {
    for {
      urlStr <- cursor.value.as[String]
      maybeUri = Uri
        .fromString(urlStr)
        .leftMap(_.getMessage())
    } yield maybeUri
  }

  /** JSON decoder for [[IRI]]. Returns an option since the URL may be malformed
    * and thus None is decoded
    */
  implicit val decodeIri: Decoder[Either[String, IRI]] = (cursor: HCursor) => {
    for {
      urlStr <- cursor.value.as[String]
    } yield IRI.fromString(urlStr)
  }
}
