package es.weso.rdfshape.server.utils.json

import io.circe.Json

/** Helper utilities to extract JSON from the complex data managed by the API.
  */
object JsonUtilsServer {

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

}
