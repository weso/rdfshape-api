package es.weso.server.results

import es.weso.server.format.DataFormat
import es.weso.utils.json.JsonUtilsServer._
import io.circe.Json

case class DataConversionResult(
    msg: String,
    data: Option[String],
    dataFormat: DataFormat,
    targetFormat: String,
    result: String
) {

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
