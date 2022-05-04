package es.weso.rdfshape.server.api.routes.schema.logic.stream.transformations

import es.weso.rdfshape.server.utils.error.exceptions.UnexpectedWebSocketFrameException
import io.circe.{Encoder, Json, ParsingFailure, parser}
import org.http4s.websocket.WebSocketFrame

/** Object containing additional utilities added to the [[WebSocketFrame]]
  * class
  */
object WebSocketTransformations {

  /** Custom encoder Encoder [[WebSocketFrame]] => [[Json]]
    *
    * If the frame contains a text message: attempt to parse to a JSON object,
    * else return an error
    *
    * @return A Circe Json object, parsed from the text inside the WebSockets
    *         message
    * @throws UnexpectedWebSocketFrameException when the WebSocket message
    *                                           received is not a text message
    * @throws ParsingFailure                    when the WebSocket message can't
    *                                           be parsed to a JSON object
    */
  implicit val encoder: Encoder[WebSocketFrame] = {
    case textMessage: WebSocketFrame.Text =>
      parser.parse(textMessage.str) match {
        case Left(err)      => throw err
        case Right(jsonObj) => jsonObj
      }
    case otherFrameType =>
      throw UnexpectedWebSocketFrameException(
        Some(otherFrameType.getClass),
        Some(WebSocketFrame.Text.getClass)
      )
  }
}
