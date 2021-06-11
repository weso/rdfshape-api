package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

trait SchemaFormat extends DataFormat {
  override val default: SchemaFormat = ShExC
}

object SchemaFormat {

  val default: SchemaFormat = ShExC

  def fromString(name: String): Either[String, SchemaFormat] =
    if(name == "") Right(default)
    else {
      formatsMap.get(name.toLowerCase) match {
        case None =>
          Left(
            s"Not found format: $name. Available formats: ${availableFormats.mkString(",")}"
          )
        case Some(df) => Right(df)
      }
    }

  private def formatsMap: Map[String, SchemaFormat] = {
    def toPair(f: SchemaFormat): (String, SchemaFormat) =
      (f.name.toLowerCase(), f)
    availableFormats.map(toPair).toMap
  }

  val availableFormats: List[SchemaFormat] =
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
case object ShExC extends SchemaFormat {
  override val name     = "shexc"
  override val mimeType = new MediaType("text", "shex")
}

case class FromDataFormat(dataFormat: DataFormat) extends SchemaFormat {
  override val name     = dataFormat.name
  override val mimeType = dataFormat.mimeType
}
