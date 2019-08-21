package es.weso.server.results

import es.weso.server.helper.DataFormat
import io.circe.Json
import es.weso.utils.json.JsonUtilsServer._

import scala.xml.Utility.escape

case class DataConversionResult(
  msg: String,
  data: Option[String],
  dataFormat: DataFormat,
  targetFormat: String,
  result: String
) {

  def toHTML: String = {
    val sb = new StringBuilder
    sb ++= "<h1>Data conversion</h1>"
    sb ++= s"<pre>${escape(result)}</pre>"
    sb ++= s"<p>Data details: <details>"
    sb ++= s"<p>Source format: ${dataFormat}. Target format: ${targetFormat}"
    sb ++= s"<pre>${escape(data.getOrElse(""))}</pre>"
    sb ++= s"</details>"
    sb.toString
  }

  def toJson: Json = Json.fromFields(
    List(
     ("msg", Json.fromString(msg)),
     ("result", Json.fromString(result)),
     ("dataFormat", Json.fromString(dataFormat.name)),
     ("targetDataFormat", Json.fromString(targetFormat))
    ) ++
    maybeField(data,"data", Json.fromString(_) )
  )
}
