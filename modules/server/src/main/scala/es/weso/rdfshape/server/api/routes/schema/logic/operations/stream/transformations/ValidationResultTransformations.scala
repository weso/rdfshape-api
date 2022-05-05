package es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.transformations

import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ContentParameter,
  TypeParameter
}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.websocket.WebSocketFrame
import org.ragna.comet.validation.result.ValidationResult

/** Object containing additional utilities added to the [[ValidationResult]]
  * class
  */
object ValidationResultTransformations {

  /** Implicit class providing extension methods for [[ValidationResult]]
    * instances
    *
    * @param validationResult Result being operated on
    */
  implicit class ValidationResultOps(validationResult: ValidationResult) {

    /** From a given comet validation result, generate its JSON representation
      * and form a WebSocket frame containing it as text
      *
      * @return A textual [[WebSocketFrame]] with the JSON representation of
      *         a [[ValidationResult]]
      */
    def toWebSocketFrame: WebSocketFrame.Text = {
      val messageJson = Json.fromFields(
        List(
          (TypeParameter.name, "result".asJson),
          (ContentParameter.name, validationResult.asJson)
        )
      )
      WebSocketFrame.Text(messageJson.spaces2)
    }

  }
}
