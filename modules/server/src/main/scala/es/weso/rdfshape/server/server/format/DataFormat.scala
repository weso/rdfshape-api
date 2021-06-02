package es.weso.rdfshape.server.server.format

import org.http4s.MediaType
import org.http4s.MediaType._

trait DataFormat extends Format {
  val default: DataFormat = Turtle
}

object DataFormat {

  val default: DataFormat = Turtle

  def fromString(name: String): Either[String, DataFormat] =
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

  private def formatsMap: Map[String, DataFormat] = {
    def toPair(f: DataFormat): (String, DataFormat) = (f.name.toLowerCase(), f)
    availableFormats.map(toPair).toMap
  }

  lazy val availableFormats: List[DataFormat] =
    List(
      Turtle,
      JsonLd,
      NTriples,
      RdfXml,
      RdfJson,
      Trig,
      HtmlMicrodata,
      HtmlRdfa11,
      Dot,
      Svg,
      Png,
      JsonDataFormat
    )

}

case object JsonDataFormat extends DataFormat {
  override val name     = "json"
  override val mimeType = new MediaType("application", "json")
}

case object Dot extends DataFormat {
  override val name     = "dot"
  override val mimeType = new MediaType("text", "vnd.graphviz")
}

case object Svg extends DataFormat {
  override val name     = "svg"
  override val mimeType = MediaType.image.`svg+xml`
}

case object Png extends DataFormat {
  override val name     = "png"
  override val mimeType = MediaType.image.png
}

sealed trait RDFFormat extends DataFormat

case object Turtle extends RDFFormat {
  override val name     = "turtle"
  override val mimeType = new MediaType("text", "turtle")
}

case object NTriples extends RDFFormat {
  override val name     = "n-triples"
  override val mimeType = new MediaType("application", "n-triples")
}

case object Trig extends RDFFormat {
  override val name     = "trig"
  override val mimeType = new MediaType("application", "trig")
}

case object JsonLd extends RDFFormat {
  override val name     = "json-ld"
  override val mimeType = new MediaType("application", "ld+json")
}

case object RdfXml extends RDFFormat {
  override val name     = "rdf/xml"
  override val mimeType = new MediaType("application", "rdf+xml")
}

case object RdfJson extends RDFFormat {
  override val name     = "rdf/json"
  override val mimeType = MediaType.application.json
}

sealed trait HtmlFormat extends DataFormat

case object HtmlRdfa11 extends HtmlFormat {
  override val name     = "html-rdfa11"
  override val mimeType = MediaType.text.html
}

case object HtmlMicrodata extends HtmlFormat {
  override val name     = "html-microdata"
  override val mimeType = MediaType.text.html
}
