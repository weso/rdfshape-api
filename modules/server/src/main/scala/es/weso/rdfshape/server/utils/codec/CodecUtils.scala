package es.weso.rdfshape.server.utils.codec

import io.circe.{Encoder, Json}
import org.http4s.Uri

/** Codecs for Circe to (de)serialize JSON data
  */
case object CodecUtils {

  /** JSON encoder for the [[Uri]] class used in http4s
    */
  implicit val uriEncoder: Encoder[Uri] = (uri: Uri) =>
    Json.fromString(uri.renderString)
}
