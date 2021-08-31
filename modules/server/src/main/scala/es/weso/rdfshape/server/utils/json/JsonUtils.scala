package es.weso.rdfshape.server.utils.json

import cats.effect.IO
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{Response, Status}

/** Helper utilities to extract JSON from the complex data managed by the API.
  */
object JsonUtils extends Http4sDsl[IO] {

  /** Utility map relating each http4s status to its generating type
    */
  private val mapStatusCodes = Map[Status, Any](
    Status.Ok                  -> Ok,
    Status.Created             -> Created,
    Status.Accepted            -> Accepted,
    Status.NoContent           -> NoContent,
    Status.Found               -> Found,
    Status.NotFound            -> NotFound,
    Status.NotAcceptable       -> NotAcceptable,
    Status.NotModified         -> NotModified,
    Status.Forbidden           -> Forbidden,
    Status.SeeOther            -> SeeOther,
    Status.BadRequest          -> BadRequest,
    Status.BadGateway          -> BadGateway,
    Status.InternalServerError -> InternalServerError
  )

  /** Converts some object to JSON, given a converter function.
    *
    * @param data Data to be converted to JSON
    * @param name Name given to the data
    * @param cnv  Converter function from A to Json
    * @tparam A Type of the data to be converted to JSON
    * @return A list with containing a single tuple: the name given to the data and the JSON representation of "A" itself.
    *         The list will be empty if no data is provided for conversion.
    */
  def maybeField[A](
      data: Option[A],
      name: String,
      cnv: A => Json
  ): List[(String, Json)] =
    data match {
      case None    => List()
      case Some(v) => List((name, cnv(v)))
    }

  /** Converts some object to JSON, given a converter function.
    *
    * @param data Data to be converted to JSON
    * @param name Name given to the data
    * @param cnv  Converter function from A to Json
    * @tparam A Type of the data to be converted to JSON
    * @return A list with containing a single tuple: the name given to the data and the JSON representation of "A" itself.
    *         In case no data is provided, the list will contain: the name given to the data and the message given instead of the data.
    */
  def eitherField[A](
      data: Either[String, A],
      name: String,
      cnv: A => Json
  ): List[(String, Json)] =
    data match {
      case Left(msg) => List((name, Json.fromString(msg)))
      case Right(v)  => List((name, cnv(v)))
    }

  /** Create a response object with a given message (will be embedded in JSON) and status code
    *
    * @param msg    Raw message that the response will contain inside a JSON
    * @param status Desired HTTP status of the response
    * @return The response object, ready to be dispatched elsewhere
    */
  def errorResponseJson(msg: String, status: Status = Ok): IO[Response[IO]] = {
    val responseMessage = mkJson(msg)
    mapStatusCodes(status) match {
      case Status.Created             => Created(responseMessage)
      case Status.Accepted            => Accepted(responseMessage)
      case Status.NoContent           => NoContent()
      case Status.Found               => Found(responseMessage)
      case Status.Forbidden           => Forbidden(responseMessage)
      case Status.SeeOther            => SeeOther(responseMessage)
      case Status.NotAcceptable       => NotAcceptable(responseMessage)
      case Status.NotModified         => NotModified()
      case Status.NotFound            => NotFound(responseMessage)
      case Status.BadRequest          => BadRequest(responseMessage)
      case Status.BadGateway          => BadGateway(responseMessage)
      case Status.InternalServerError => InternalServerError(responseMessage)

      case _ => Ok(responseMessage)
    }
  }

  private def mkJson(msg: String): Json =
    Json.fromFields(List(("error", Json.fromString(msg))))

}
