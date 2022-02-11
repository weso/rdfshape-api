package es.weso.rdfshape.server.api.routes.data.logic.aux

import es.weso.rdf.InferenceEngine
import es.weso.rdfshape.server.api.definitions.ApiDefaults
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Implicit encoders and decoders for [[es.weso.rdf.InferenceEngine]] instances,
  * used when encoding and decoding [[Data]] instances
  */
private[data] object InferenceCodecs {

  /** Auxiliary encoder for data inference.
    */
  implicit val encodeInference: Encoder[InferenceEngine] =
    (inference: InferenceEngine) => Json.fromString(inference.name)

  /** Auxiliary decoder for data inference
    */
  implicit val decodeInference: Decoder[InferenceEngine] =
    (cursor: HCursor) =>
      for {
        inferenceName <- cursor.value.as[String]
        inference = InferenceEngine
          .fromString(inferenceName)
          .toOption
          .getOrElse(ApiDefaults.defaultInferenceEngine)
      } yield inference

}
