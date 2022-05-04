package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect._
import cats.effect.std.Queue
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.{
  ShExFormat,
  ShaclFormat
}
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.schema.logic.operations.{
  SchemaConvert,
  SchemaInfo,
  SchemaValidate
}
import es.weso.rdfshape.server.api.routes.schema.logic.stream.transformations.CometTransformations.toValidationStream
import es.weso.rdfshape.server.api.routes.schema.logic.stream.transformations.ValidationResultTransformations.ValidationResultOps
import es.weso.rdfshape.server.api.routes.schema.logic.stream.transformations.WebSocketTransformations.encoder
import es.weso.rdfshape.server.api.routes.schema.service.WebSocketClosures._
import es.weso.rdfshape.server.api.routes.schema.service.operations.SchemaConvertInput.decoder
import es.weso.rdfshape.server.api.routes.schema.service.operations.stream.SchemaValidateStreamInput
import es.weso.rdfshape.server.api.routes.schema.service.operations.{
  SchemaConvertInput,
  SchemaInfoInput,
  SchemaValidateInput
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.SchemaEngineParameter
import es.weso.rdfshape.server.implicits.string_parsers.instances.schemaEngineParser
import es.weso.schema.{
  JenaShacl,
  Schemas,
  ShExSchema,
  ShaclTQ,
  ShaclexSchema,
  Schema => SchemaW
}
import io.circe.syntax.EncoderOps
import io.circe.{DecodingFailure, Json, ParsingFailure}
import fs2._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.ragna.comet.exception.stream.timed.StreamTimeoutException
import org.ragna.comet.exception.stream.validations.{
  StreamErroredItemException,
  StreamInvalidItemException,
  StreamValidationException
}

/** API service to handle schema-related operations
  *
  * @param client    HTTP4S client object
  * @param wsBuilder Builder to operate WebSocket connections on this
  *                  service
  */
class SchemaService(client: Client[IO], wsBuilder: WebSocketBuilder[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  /** Final WebSockets connection builder used in this service.
    * Privately customize the WebSocket builder's ([[wsBuilder]]) responses
    * on certain events
    */
  private lazy val webSocketsBuilder = wsBuilder
    .withOnNonWebSocketRequest(
      BadRequest(SchemaServiceError.nonWebSocketRequestError)
    )
    .withOnHandshakeFailure(
      InternalServerError(SchemaServiceError.nonWebSocketHandshakeError)
    )
  override val verb: String = "schema"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    s"""Get an array with the accepted schema engines 
       | given an schema type (${SchemaServiceDescriptions.SchemaType.schemaTypes
      .mkString(" or ")})""".stripMargin **
      GET / `verb` / "engines" / pathVar[String](
        SchemaServiceDescriptions.SchemaType.name,
        SchemaServiceDescriptions.SchemaType.description
      ) |>> { `type`: String =>
        val engines = `type`.toLowerCase match {
          case "shex" => List(Schemas.shEx)
          case "shacl" =>
            List(Schemas.shaclex, Schemas.jenaShacl, Schemas.shaclTQ)
        }
        val json = Json.fromValues(
          engines.map(_.name).map(str => Json.fromString(str))
        )
        Ok(json)
      }

    "Get the default schema engine as a raw string" **
      GET / `verb` / "engines" / "default" |>> {
        Ok(Schemas.defaultSchema.name)
      }

    "Get the default schema format for a given schema engine as a raw string" **
      GET / `verb` / "formats" / "default" / pathVar[SchemaW](
        SchemaServiceDescriptions.SchemaEngine.name,
        SchemaServiceDescriptions.SchemaEngine.description
      ) |>> { engine: SchemaW =>
        val defaultFormat = engine match {
          case ShExSchema(_) => ShExFormat.default
          case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
            ShaclFormat.default
        }
        Ok(defaultFormat.name)
      }

    "Get an array with the accepted schema formats for a given schema engine" **
      GET / `verb` / "formats" / pathVar[SchemaW](
        SchemaServiceDescriptions.SchemaEngine.name,
        SchemaServiceDescriptions.SchemaEngine.description
      ) |>> { engine: SchemaW =>
        val formats = engine match {
          case ShExSchema(_) => ShExFormat.availableFormats
          case JenaShacl(_) | ShaclTQ(_) | ShaclexSchema(_) =>
            ShaclFormat.availableFormats
        }
        val json = Json.fromValues(formats.map(f => Json.fromString(f.name)))
        Ok(json)
      }

    "Get an array with the accepted Trigger Modes for validations" **
      GET / `verb` / "triggerModes" |>> {
        val json = Json.fromValues(
          ApiDefinitions.availableTriggerModes.map(
            Json.fromString
          )
        )
        Ok(json)
      }

    "Obtain information about an schema: list of shapes and prefix map" **
      POST / `verb` / "info" ^ jsonOf[IO, SchemaInfoInput] |>> {
        body: SchemaInfoInput =>
          SchemaInfo
            .schemaInfo(body.schema)
            .flatMap(info => Ok(info.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))

      }

    "Convert a given schema to another format (this includes graphic formats for visualizations)" **
      POST / `verb` / "convert" ^ jsonOf[IO, SchemaConvertInput] |>> {
        body: SchemaConvertInput =>
          SchemaConvert
            .schemaConvert(
              body.schema,
              body.targetFormat,
              body.targetEngine
            )
            .flatMap(result => Ok(result.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "Validates RDF data against a given schema" **
      POST / `verb` / "validate" ^ jsonOf[IO, SchemaValidateInput] |>> {
        body: SchemaValidateInput =>
          SchemaValidate
            .schemaValidate(body.data, body.schema, body.triggerMode)
            .flatMap(result => Ok(result.asJson))
            .handleErrorWith(err => InternalServerError(err.getMessage))
      }

    "WebSockets endpoint meant for streaming validations" **
      GET / `verb` / "validate" / "stream" |>> {

        /* Stream validations are done via WebSockets. The basic flow goes as
         * follows:
         * 1. The client starts a WebSocket connection.
         * 2. The client sends a message to the server, saying that it wants the
         * server to validate some data and passing that data to the server.
         * 3. The server starts validating and returns the output validation
         * stream as WebSocket messages.
         * --------------------------------------------------------------------
         * Note that, to do this, we must connect the input and the output
         * WebSocket messages (the server needs the information from the client
         * message to start generating output messages).
         *
         * Therefore, we use a Queue scoped to each request, as follows:
         * 1. Each client incoming message is enqueued.
         * 2. The server dequeues the message in search for instructions to
         * validate data, errors are handled at the end of the stream and cause
         * the connection to close (error reason is attached) */

        /* Inspired by the examples in:
         * https://github.com/http4s/http4s/blob/756c8940ca0161e940b691adc5ea59060d444417/examples/blaze/src/main/scala/com/example/http4s/blaze/BlazeWebSocketExample.scala */
        Queue
          .unbounded[IO, Option[WebSocketFrame]]
          .flatMap { queue =>
            // Stream of messages from the server to the client
            val toClient: Stream[IO, WebSocketFrame] =
              Stream
                /* 0. Get input data stream.
                 * Poll the queue to see if new messages arrived from the client */
                .fromQueueNoneTerminated(queue)
                /* 1. WebSocketFrame => JSON.
                 * Mapping: attempt to parse JSON out of the client message */
                .map(_.asJson)
                /* 2. JSON => SchemaValidateStreamInput.
                 * Mapping: attempt to get the validation settings from the
                 * client's JSON message.
                 * If decoding errors arise: throw */
                .evalMap(_.as[SchemaValidateStreamInput] match {
                  case Left(err)    => IO.raiseError(err)
                  case Right(value) => IO.pure(value)
                })
                /* 3. Stream[IO, SchemaValidateStreamInput] =>
                 * Stream[IO,ValidationResult].
                 * Pipe: create a comet validation stream from the parsed
                 * configuration.
                 * If the validation begins, a stream of results will be
                 * returned */
                .through(toValidationStream)
                /* 4. ValidationResult => WebSocketFrame.
                 * Mapping: convert each validation result to a WebSocketFrame
                 * object containing the results in its text */
                .map(_.toWebSocketFrame)
                /* 5. Error processing.
                 * Catch all known exceptions and generate a closing frame
                 * depending on the error.
                 * Close the WebSockets stream with custom error codes. */
                .handleErrorWith(err =>
                  Stream.eval(IO {
                    // Print debug info
                    println(err)
                    err.printStackTrace()
                    err match {
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
                      // Other, unknown error
                      case _ =>
                        unknownErrorClosure.closingFrame
                    }
                  })
                /* 6. Graceful stop.
                 * If the validations stream comes to an end, concatenate a
                 * graceful closing frame */
                ) ++ Stream.eval(IO(standardClosure.closingFrame))

            /* Pipe for processing the stream of messages from the client to the
             * server:
             * - Put all incoming client messages into the queue so that the
             * server processes them */
            val fromClient: Pipe[IO, WebSocketFrame, Unit] =
              _.enqueueNoneTerminated(queue)

            webSocketsBuilder.build(toClient, fromClient)
          }
      }
  }

}

object SchemaService {

  /** Service factory
    *
    * @param client    Underlying http4s client
    * @param wsBuilder Underlying webSockets builder
    * @return A new Schema Service
    */
  def apply(
      client: Client[IO],
      wsBuilder: WebSocketBuilder[IO]
  ): SchemaService =
    new SchemaService(client, wsBuilder)
}

/** Compendium of additional text constants used on service errors
  */
private object SchemaServiceError {

  /** Placeholder error message used when an unknown error occurred
    * in a data validation
    */
  val couldNotValidateData =
    "Unknown error validating the data provided. Check the inputs."

  /** Error message used when the WebSockets endpoint is contacted with a
    * non-websockets request
    */
  val nonWebSocketRequestError =
    "A request was received but it was not a valid WebSocket request."

  /** Error message used when the WebSockets handshake fails
    */
  val nonWebSocketHandshakeError =
    "An error occurred during the WebSockets handshake process."
}

/** Set of constants/utils used for closing connections
  * in the WebSockets service endpoint
  */
private object WebSocketClosures {

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
    WebSocketClosure(3000, "The client message did not contain valid JSON data")

  /** Generate a closing frame with an assigned error code,
    * and an informational message
    *
    * Meant to be used when data sent by the client was readable but its contents
    * were not valid
    */
  def invalidConfigurationClosure: WebSocketClosure =
    WebSocketClosure(
      3001,
      "The client message did not contain a valid configuration"
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
      "Connection closed because an item was invalid"
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
      "Connection closed because an error occurred while validating an item"
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
      "Connection closed because no items were received for a while"
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

/** Compendium of additional text constants used to describe inline parameters
  * (query and path parameters) in Swagger
  */
private object SchemaServiceDescriptions {
  case object SchemaEngine {
    val name: String = SchemaEngineParameter.name
    val description =
      s"Engine in which the validation schema is redacted. One of: ${ApiDefinitions.availableSchemaEngines.map(_.name).mkString(", ")}"
  }

  case object SchemaType {
    val schemaTypes = List("ShEx", "SHACL")
    val name        = "SchemaType"
    val description =
      s"Type of the validation schema. One of: ${schemaTypes.mkString(", ")}"
  }
}
