package es.weso.rdfshape.server.api.routes.data.logic

import es.weso.rdfshape.server.api.format.DataFormat
import es.weso.rdfshape.server.utils.json.JsonUtils.maybeField
import io.circe.Json

/** Data class representing the output of a conversion operation
  *
  * @param msg          Output informational message after conversion
  * @param data         Data to be converted
  * @param dataFormat   Initial data format
  * @param targetFormat Target data format
  * @param result       Data after conversion
  */
case class DataConversionResult(
    msg: String,
    data: Option[String],
    dataFormat: DataFormat,
    targetFormat: String,
    result: String
) {

  /** Convert a conversion result to its JSON representation
    * @return JSON representation of the conversion result
    */
  def toJson: Json = Json.fromFields(
    List(
      ("msg", Json.fromString(msg)),
      ("result", Json.fromString(result)),
      ("dataFormat", Json.fromString(dataFormat.name)),
      ("targetDataFormat", Json.fromString(targetFormat))
    ) ++
      maybeField(data, "data", Json.fromString)
  )
}
