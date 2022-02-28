package es.weso.rdfshape.server.api.format.dataFormats

import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import org.http4s.MediaType

/** Extension of the Format interface to represent generic data formats (RDF, schema, shapeMaps...)
  */
class DataFormat(formatName: String, formatMimeType: MediaType) extends Format {
  override val name: String        = formatName
  override val mimeType: MediaType = formatMimeType
}

/** Companion object with all DataFormat static utilities
  */
object DataFormat extends FormatCompanion[DataFormat] {

  override lazy val availableFormats: List[DataFormat] =
    (RdfFormat.availableFormats ++
      SchemaFormat.availableFormats ++
      HtmlFormat.availableFormats ++
      GraphicFormat.availableFormats ++
      ShapeMapFormat.availableFormats ++
      List(Json, Dot)).distinct
  override val default: DataFormat = Json
}

/** Represents the mime-type "application/json"
  */
case object Json
    extends DataFormat(
      formatName = "JSON",
      formatMimeType = new MediaType("application", "json")
    )

/** Represents the mime-type "text/vnd.graphviz", used by graphviz
  */
case object Dot
    extends DataFormat(
      formatName = "DOT",
      formatMimeType = new MediaType("text", "vnd.graphviz")
    )
