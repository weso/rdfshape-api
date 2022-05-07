package es.weso.rdfshape.server.api.routes.schema.logic.operations.stream

import cats.effect.IO
import cats.effect.std.Queue
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.WebSocketClosures._
import es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.transformations.CometTransformations.toValidationStream
import es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.transformations.ValidationResultTransformations.ValidationResultOps
import es.weso.rdfshape.server.api.routes.schema.logic.operations.stream.transformations.WebSocketTransformations.encoder
import es.weso.rdfshape.server.api.routes.schema.service.SchemaService
import es.weso.rdfshape.server.api.routes.schema.service.operations.SchemaValidateStreamInput
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ContentParameter,
  MessageParameter,
  ReasonParameter,
  TypeParameter
}
import fs2.Stream
import io.circe.syntax.EncoderOps
import io.circe.{DecodingFailure, Json, ParsingFailure}
import org.apache.kafka.common.KafkaException
import org.http4s.websocket.WebSocketFrame
import org.ragna.comet.exception.stream.timed.StreamTimeoutException
import org.ragna.comet.exception.stream.validations.{
  StreamErroredItemException,
  StreamInvalidItemException,
  StreamValidationException
}
import org.ragna.comet.validation.result.ValidationResult

object StreamValidation extends LazyLogging {

  /** Given an input queue with WebSocket messages, parse these messages in search
    * for instructions to validate data and, if available, start running a
    * Comet validation stream whose output will be sent to the WebSockets client.
    *
    * This is the core of the [[SchemaService]] stream validation service and
    * further info can be found there.
    *
    * Log messages are registered along the way.
    *
    * @param queue Queue structure from which this stream will read WebSocket
    *              messages
    * @return A Stream of WebSocket frames sent back to the client, each of
    *         them containing the results of a validation in JSON format.
    * @note The workflow is defined in the in-line comments
    */
  def mkValidationStream(
      queue: Queue[IO, Option[WebSocketFrame]]
  ): Stream[IO, WebSocketFrame] = {
    Stream
      /* 0. Get input data stream.
       * Poll the queue to see if new messages arrived from the client */
      .fromQueueNoneTerminated(queue)
      .evalTap(_ =>
        IO(logger.info("Begun streaming validation through WebSockets"))
      )
      /* 1. WebSocketFrame => JSON.
       * Mapping: attempt to parse JSON out of the client message */
      .map(_.asJson)
      /* 2. JSON => SchemaValidateStreamInput.
       * Mapping: attempt to get the validation settings from the client's JSON
       * message.
       * If decoding errors arise: throw */
      .evalMap(_.as[SchemaValidateStreamInput] match {
        case Left(err)    => IO.raiseError(err)
        case Right(value) => IO.pure(value)
      })
      .evalTap(_ =>
        IO(logger.debug("Decoded client's streaming validation request"))
      )
      /* 3. Stream[IO, SchemaValidateStreamInput] =>
       * Stream[IO,ValidationResult].
       * Pipe: create a comet validation stream from the parsed configuration.
       * If the validation begins, a stream of results will be returned */
      .through(toValidationStream)
      /* 4. ValidationResult => WebSocketFrame.
       * Mapping: convert each validation result to a WebSocketFrame object
       * containing the results in its text */
      .map(_.toWebSocketFrame)
      /* 5. Error processing.
       * Catch all known exceptions and generate two frames:
       * 1. A text frame with the detailed error cause.
       * 2. A closing frame with code and reason dependant on the error. */
      .handleErrorWith(err => {
        Stream.evalSeq(IO {
          logger.debug(s"Interrupted streaming validation due to err: $err")
          List(mkErrorFrame(err), mkClosingFrame(err))
        })
        /* 6. Graceful stop.
         * If the validations stream comes to an end, concatenate a graceful
         * closing frame */
      }) ++ Stream
      .eval(IO(standardClosure.closingFrame))
      .onFinalize(
        IO(logger.info("Ended streaming validation through WebSockets"))
      )
  }

  /** Given an error/exception, produce a WebSockets frame explaining it
    *
    * The possibility to send messages with a detailed error cause is because
    * closing frames are too limited in size to attach the full error info
    *
    * @param error Input error
    * @return WebSocket text frame with information about the input error
    * @note If the error is a [[StreamInvalidItemException]], we try to include
    *       the invalid validation report instead of a generic message
    */
  private def mkErrorFrame(error: Throwable): WebSocketFrame.Text = {

    val contentJson: Json = Json
      .obj(
        (
          MessageParameter.name,
          s"${error.getClass.getSimpleName} - ${error.getMessage}".asJson
        ),
        (
          ReasonParameter.name,
          /* If the error can give us further information, return here in
           * "reason", else null value */
          error match {
            case invalidErr: StreamInvalidItemException
                if invalidErr.reason.isDefined =>
              invalidErr.reason
                .map(ValidationResult.getValidationReportJson)
                .get
            case _ => Json.Null
          }
        )
      )
      .dropNullValues

    val messageJson = Json.fromFields(
      List(
        (TypeParameter.name, "error".asJson),
        (ContentParameter.name, contentJson)
      )
    )

    WebSocketFrame.Text(messageJson.spaces2)
  }

  /** Given an error/exception, produce a WebSockets closing frame
    *
    * @param error Input error
    * @return WebSocket closing frame whose code and reason are generated
    *         depending on the input error
    */
  private def mkClosingFrame(error: Throwable): WebSocketFrame.Close =
    error match {
      case _: ParsingFailure =>
        invalidJsonClosure.closingFrame
      case _: DecodingFailure =>
        invalidConfigurationClosure.closingFrame
      case sve: StreamValidationException =>
        sve match {
          case _: StreamInvalidItemException =>
            invalidItemClosure.closingFrame
          case _: StreamErroredItemException =>
            erroredItemClosure.closingFrame
        }
      case _: StreamTimeoutException =>
        timeoutClosure.closingFrame
      case _: IllegalArgumentException =>
        illegalArgumentClosure.closingFrame
      case _: AssertionError =>
        assertionClosure.closingFrame
      case _: KafkaException =>
        kafkaClosure.closingFrame
      case _ =>
        unknownErrorClosure.closingFrame
    }
}

/** Set of constants/utils used for closing connections
  * in the WebSockets streaming validation
  */
private[stream] object WebSocketClosures {

  /** Generate a closing frame with the default code for success
    * and an informational message
    */
  def standardClosure: WebSocketClosure =
    WebSocketClosure(1000, "Connection finished")

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when data sent by the client was not readable as JSON
    */
  def invalidJsonClosure: WebSocketClosure =
    WebSocketClosure(3000, "The message did not contain valid JSON data")

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when data sent by the client was readable but its contents
    * were not valid
    */
  def invalidConfigurationClosure: WebSocketClosure =
    WebSocketClosure(
      3001,
      "The message did not contain a valid configuration"
    )

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when the validation stopped because an item was invalid
    * and was configured to do so
    */
  def invalidItemClosure: WebSocketClosure =
    WebSocketClosure(
      3002,
      "An item was invalid"
    )

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when the validation stopped because an item validation
    * threw an error and was configured to do so
    */
  def erroredItemClosure: WebSocketClosure =
    WebSocketClosure(
      3003,
      "An error occurred while validating an item"
    )

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when the validation stopped because no items were
    * validated for some time
    */
  def timeoutClosure: WebSocketClosure =
    WebSocketClosure(
      3004,
      "No items were received for a while"
    )

  def illegalArgumentClosure: WebSocketClosure =
    WebSocketClosure(
      3005,
      "The configuration contained invalid values"
    )

  def assertionClosure: WebSocketClosure =
    WebSocketClosure(
      3006,
      "An invalid value was provided to the server"
    )

  def kafkaClosure: WebSocketClosure =
    WebSocketClosure(
      3007,
      "An error occurred connecting to the Kafka stream"
    )

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when an unexpected error occurred
    */
  def unknownErrorClosure: WebSocketClosure =
    WebSocketClosure(
      4999,
      "Connection closed due to an unknown error"
    )

  /** Data required to create a WebSocket closing frame, specially used for
    * erroring connection closures
    *
    * @param code   Code sent when closing the connection
    * @param reason Reason why the connection is closed
    * @note [[reason]] should be kept as short as possible due to size limitations
    * @see [[https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close#parameters]]
    */
  case class WebSocketClosure(code: Int, reason: String) {

    /** Create a WebSocket closing frame with this instance's code and reason.
      *
      * In case of errors, resort to the default closing frame.
      */
    lazy val closingFrame: WebSocketFrame.Close = WebSocketFrame
      .Close(code, reason)
      .getOrElse(WebSocketFrame.Close())
  }
}
