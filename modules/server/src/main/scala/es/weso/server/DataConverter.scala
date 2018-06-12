package es.weso.server

import java.io.ByteArrayOutputStream

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema.DataFormats
import es.weso.server.APIService.logger
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter

import scala.util.Try

object DataConverter extends LazyLogging {

  private[server] def dotConverter(dot: String, targetFormat: Format): Either[String,String] = {
    logger.info(s"dotConverter to $targetFormat. dot\n$dot")
    println(s"targetFormat: $targetFormat")
    Try {
      val g: MutableGraph = Parser.read(dot)
      targetFormat match {
        case Format.SVG => {
          val renderer = Graphviz.fromGraph(g) //.width(200)
            .render(targetFormat)
          logger.info(s"SVG converted: ${renderer.toString}")
          renderer.toString
        }
        case Format.PNG => {
          val renderer = Graphviz.fromGraph(g).
            render(Format.PNG)
          val image = renderer.toImage
          val baos = new ByteArrayOutputStream()
          ImageIO.write(image, "png", baos)
          val data = DatatypeConverter.printBase64Binary(baos.toByteArray)
          val imageString = "data:image/png;base64," + data
          "<html><body><img src='" + imageString + "'></body></html>"
        }
        case _ => s"Error converting to $targetFormat"
      }

    }.fold(
      e => Left(e.getMessage),
      s => Right(s)
    )
  }

  private def getTargetFormat(str: String): Either[String,Format] = str.toUpperCase match {
    case "SVG" => Right(Format.SVG)
    case "PNG" => Right(Format.PNG)
    case "PS" => Right(Format.PS)
    case _ => Left(s"Unsupported format $str")
  }

  lazy val dataFormats = RDFAsJenaModel.availableFormats.map(_.toUpperCase)
  lazy val availableGraphFormats = List(
    GraphFormat("SVG","application/svg",Format.SVG),
    GraphFormat("PNG","application/png",Format.PNG),
    GraphFormat("PS","application/ps",Format.PS)
  )
  lazy val availableFormats = availableGraphFormats.map(_.name)

  case class GraphFormat(name: String, mime: String, fmt: Format)


  private[server] def dataConvert(data: String,
                          optDataFormat: Option[String],
                          optTargetFormat: Option[String]
                         ): Either[String,DataConversionResult] = {
    val dataFormat = optDataFormat.getOrElse(DataFormats.defaultFormatName)
    val targetFormat = optTargetFormat.getOrElse(DataFormats.defaultFormatName)
    logger.info(s"Converting $data with format $dataFormat to $targetFormat")
    for {
      rdf <- RDFAsJenaModel.fromChars(data,dataFormat,None)
      result <- rdfConvert(rdf,targetFormat)
    } yield DataConversionResult(data,dataFormat,targetFormat,result)
  }

  private[server] def rdfConvert(rdf: RDFReasoner,
                                 targetFormat: String
                                 ): Either[String,String] =
  targetFormat.toUpperCase match {
      case t if dataFormats.contains(t) => rdf.serialize(t)
      case t if availableFormats.contains(t) => for {
        fmt <- getTargetFormat(t)
        dot <- rdf.serialize("DOT")
        outstr <- dotConverter(dot,fmt)
      } yield outstr
      case _ =>
        Left(s"Unsupported conversion to $targetFormat\nFormats available: ${(dataFormats ++ availableFormats).mkString(",")}")
  }

}