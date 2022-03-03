package es.weso.rdfshape.server.utils.json

import cats.effect.IO
import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
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
    * @param name      Name given to the data
    * @param data      Data to be converted to JSON
    * @param converter Converter function from A to Json
    * @tparam A Type of the data to be converted to JSON
    * @return A list with containing a single tuple: the name given to the data and the JSON representation of "A" itself.
    *         The list will be empty if no data is provided for conversion.
    */
  def maybeField[A](
      name: String,
      data: Option[A],
      converter: A => Json
  ): List[(String, Json)] =
    data match {
      case None    => List()
      case Some(v) => List((name, converter(v)))
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
  def errorResponseJson(
      msg: String,
      status: Status = InternalServerError
  ): IO[Response[IO]] = {
    val responseMessage = mkJsonError(msg)
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

  private def mkJsonError(msg: String): Json =
    Json.fromFields(List(("error", Json.fromString(msg))))

  /** Convert a given prefix map to JSON format for API operations
    *
    * @param prefixMap Input prefix map
    * @return JSON representation of the prefix map (as an object)
    * @note Example return: {
    *       "schema": "<http://schema.org/>",
    *       "xsd: "<http://www.w3.org/2001/XMLSchema#>"
    *       }
    */
  def prefixMap2JsonObject(prefixMap: PrefixMap): Json = {
    Json.fromFields(prefixMap.pm.map { case (prefix, iri) =>
      (prefix.str, Json.fromString(iri.getLexicalForm))
    })
  }

  /** Convert a given prefix map to JSON format for API operations
    *
    * @param prefixMap Input prefix map
    * @return JSON representation of the prefix map (as an array of items)
    * @note Example return:
    *    [
    *       {
    *        "prefixName": "schema",
    *        "prefixIRI": "<http://schema.org/>",
    *       },
    *       {
    *        "prefixName": "xsd",
    *        "prefixIRI": "<http://www.w3.org/2001/XMLSchema#>",
    *       }
    *    ]
    */
  def prefixMap2JsonArray(prefixMap: PrefixMap): Json = {
    Json.fromValues(prefixMap.pm.map { case (prefix, iri) =>
      Json.fromFields(
        List(
          (
            "prefixName",
            Json.fromString(if(prefix.str.isBlank) ":" else prefix.str)
          ),
          ("prefixIRI", Json.fromString(iri.toString()))
        )
      )
    })
  }

  /** @param iri      IRI to be converted
    * @param prefixMap Optionally, the prefix map with the IRI to be converted
    * @return JSON representation of the IRI
    */
  def iri2Json(iri: IRI, prefixMap: Option[PrefixMap]): Json = {
    prefixMap match {
      case Some(pm) => Json.fromString(pm.qualifyIRI(iri))
      case None     => Json.fromString(iri.toString)
    }

  }

}
