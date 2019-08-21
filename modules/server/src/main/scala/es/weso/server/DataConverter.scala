package es.weso.server

import java.io.ByteArrayOutputStream
import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.sgraph.{RDF2SGraph, RDFDotPreferences}
import es.weso.server.DataConverter.GraphFormat
import es.weso.server.helper.DataFormat
import es.weso.server.results.DataConversionResult
import guru.nidi.graphviz.engine.{Format, Graphviz}
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import javax.imageio.ImageIO

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
          val data = Base64.getEncoder.encodeToString(baos.toByteArray)
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

  private lazy val rdfDataFormats = RDFAsJenaModel.availableFormats.map(_.toUpperCase)
  private lazy val availableGraphFormats = List(
    GraphFormat("SVG","application/svg",Format.SVG),
    GraphFormat("PNG","application/png",Format.PNG),
    GraphFormat("PS","application/ps",Format.PS)
  )

  lazy val availableGraphFormatNames = availableGraphFormats.map(_.name)

  private case class GraphFormat(name: String, mime: String, fmt: Format)

  private[server] def dataConvert(data: String,
                                  dataFormat: DataFormat,
                                  targetFormat: String
                                 ): Either[String,DataConversionResult] = {
    println(s"Converting $data with format $dataFormat to $targetFormat. OptTargetFormat: $targetFormat")
    for {
      rdf <- RDFAsJenaModel.fromChars(data,dataFormat.name,None)
      result <- rdfConvert(rdf,Some(data),dataFormat,targetFormat)
    } yield result
  }

  private[server] def rdfConvert(rdf: RDFReasoner,
                                 data: Option[String],
                                 dataFormat: DataFormat,
                                 targetFormat: String
                                 ): Either[String,DataConversionResult] = for {
    converted <- targetFormat.toUpperCase match {
      case "JSON" => for {
        sgraph <- RDF2SGraph.rdf2sgraph(rdf)
      } yield sgraph.toJson.spaces2
      case "DOT" => for {
        sgraph <- RDF2SGraph.rdf2sgraph(rdf)
      } yield sgraph.toDot(RDFDotPreferences.defaultRDFPrefs)
      case t if rdfDataFormats.contains(t) => rdf.serialize(t)
      case t if availableGraphFormatNames.contains(t) =>for {
        sgraph <- RDF2SGraph.rdf2sgraph(rdf)
        format <- getTargetFormat(t)
        dotStr = sgraph.toDot(RDFDotPreferences.defaultRDFPrefs)
        converted <- dotConverter(dotStr,format)
      } yield converted
      case t => Left(s"Unsupported format: ${t}")
    }
  } yield DataConversionResult("Conversion successful!",data, dataFormat, targetFormat, converted)


}
