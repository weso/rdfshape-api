package es.weso.server.format

import org.http4s.MediaType
import org.http4s.MediaType._

trait SchemaFormat extends DataFormat {
  override val default: SchemaFormat = ShExC 
 
  override val availableFormats: List[SchemaFormat] =
    List(
      FromDataFormat(Turtle),
      FromDataFormat(JsonLd),
      FromDataFormat(NTriples),
      FromDataFormat(RdfXml),
      FromDataFormat(RdfJson),
      FromDataFormat(Trig),
      ShExC
    )

}

object SchemaFormat {
  val default: SchemaFormat = ShExC

  def fromString(s: String): Either[String, SchemaFormat] = default.fromString(s)
}

case object ShExC extends SchemaFormat {
  override val name = "shexc"
  override val mimeType = new MediaType("text","shex")
}


case class FromDataFormat(dataFormat: DataFormat) extends SchemaFormat {
  override val name = dataFormat.name
  override val mimeType = dataFormat.mimeType
}
