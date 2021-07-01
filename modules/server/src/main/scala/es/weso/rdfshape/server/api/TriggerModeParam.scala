package es.weso.rdfshape.server.api

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.Defaults._
import es.weso.shapemaps.ShapeMap

case class TriggerModeParam(
    triggerMode: Option[String],
    shapeMap: Option[String],
    shapeMapFormatTextarea: Option[String],
    shapeMapURL: Option[String],
    shapeMapFormatUrl: Option[String],
    shapeMapFile: Option[String],
    shapeMapFormatFile: Option[String],
    activeShapeMapTab: Option[String]
) extends LazyLogging {

  val shapeMapFormat: Option[String] = parseShapeMapTab(
    activeShapeMapTab.getOrElse(defaultActiveShapeMapTab)
  ) match {
    case Right(`shapeMapUrlType`)      => shapeMapFormatUrl
    case Right(`shapeMapFileType`)     => shapeMapFormatFile
    case Right(`shapeMapTextAreaType`) => shapeMapFormatTextarea
    case _                             => None
  }

  def getShapeMap(
      nodesPrefixMap: PrefixMap,
      shapesPrefixMap: PrefixMap
  ): IO[(Option[String], Either[String, ShapeMap])] = {
    val inputType = parseShapeMapTab(
      activeShapeMapTab.getOrElse(defaultActiveShapeMapTab)
    )
    logger.debug(s"input type: $inputType")
    inputType match {
      case Right(`shapeMapUrlType`) =>
        logger.debug(s"ShapeMap input type: shapeMapUrlType")

        shapeMapURL match {
          case None => IO.pure((None, Left(s"No value for shapeMapURL")))
          case Some(shapeMapUrl) =>
            logger.trace(s"ShapeMapUrl: $shapeMapUrl")
            
            val shapeMapFormat =
              shapeMapFormatUrl.getOrElse(defaultShapeMapFormat)
            ShapeMap
              .fromURI(
                shapeMapUrl,
                shapeMapFormat,
                None,
                nodesPrefixMap,
                shapesPrefixMap
              )
              .map {
                case Left(str) =>
                  (
                    None,
                    Left(
                      s"Error obtaining $shapeMapUrl with $shapeMapFormat: $str"
                    )
                  )
                case Right(shapeMap) =>
                  (Some(shapeMap.toString), Right(shapeMap))
              }
        }
      case Right(`shapeMapFileType`) =>
        logger.debug(s"ShapeMap input type: shapeMapFileType")

        shapeMapFile match {
          case None => IO.pure((None, Left(s"No value for shapeMapFile")))
          case Some(shapeMapStr) =>
            logger.trace(s"ShapeMapFile: $shapeMapStr")

            val shapeMapFormat =
              shapeMapFormatFile.getOrElse(defaultShapeMapFormat)
            ShapeMap.fromString(shapeMapStr, shapeMapFormat, None) match {
              case Left(ls) =>
                IO.pure((Some(shapeMapStr), Left(ls.toList.mkString("\n"))))
              case Right(parsedShapeMap) =>
                IO.pure((Some(shapeMapStr), Right(parsedShapeMap)))
            }
        }
      case Right(`shapeMapTextAreaType`) =>
        logger.debug(s"ShapeMap input type: shapeMapTextAreType")

        shapeMap match {
          case None => IO.pure((None, Right(ShapeMap.empty)))
          case Some(shapeMapStr) =>
            logger.trace(s"ShapeMapText: $shapeMapStr")

            val shapeMapFormat =
              shapeMapFormatTextarea.getOrElse(defaultShapeMapFormat)
            ShapeMap.fromString(shapeMapStr, shapeMapFormat, None) match {
              case Left(ls) =>
                IO.pure((Some(shapeMapStr), Left(ls.toList.mkString("\n"))))
              case Right(parsedShapeMap) =>
                IO.pure((Some(shapeMapStr), Right(parsedShapeMap)))
            }
        }
      case Right(other) =>
        val msg = s"Unknown value for activeShapeMapTab: $other"
        logger.warn(msg)
        IO.pure((None, Left(msg)))
      case Left(msg) => IO.pure((None, Left(msg)))
    }
  }

  def parseShapeMapTab(tab: String): Either[String, ShapeMapInputType] = {
    val inputTypes =
      List(shapeMapUrlType, shapeMapFileType, shapeMapTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None =>
        Left(
          s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]"
        )
    }
  }

  sealed abstract class ShapeMapInputType {
    val id: String
  }

  case object shapeMapUrlType extends ShapeMapInputType {
    override val id = "#shapeMapUrl"
  }

  case object shapeMapFileType extends ShapeMapInputType {
    override val id = "#shapeMapFile"
  }

  case object shapeMapTextAreaType extends ShapeMapInputType {
    override val id = "#shapeMapTextArea"
  }

}

object TriggerModeParam extends LazyLogging {

  def mkTriggerModeParam(partsMap: PartsMap): IO[TriggerModeParam] = {
    val tp: IO[TriggerModeParam] = for {
      optTriggerMode  <- partsMap.optPartValue("triggerMode")
      optShapeMap     <- partsMap.optPartValue("shapeMap")
      optShapeMapURL  <- partsMap.optPartValue("shapeMapURL")
      optShapeMapFile <- partsMap.optPartValue("shapeMapFile")
      optShapeMapFormatTextArea <- partsMap.optPartValue(
        "shapeMapFormatTextArea"
      )
      optShapeMapFormatUrl  <- partsMap.optPartValue("shapeMapFormatURL")
      optShapeMapFormatFile <- partsMap.optPartValue("shapeMapFormatFile")
      optActiveShapeMapTab  <- partsMap.optPartValue("shapeMapActiveTab")
    } yield {
      logger.debug(s"optTriggerMode: $optTriggerMode")
      logger.debug(s"optShapeMap: $optShapeMap")
      logger.debug(s"optActiveShapeMapTab: $optActiveShapeMapTab")
      logger.debug(s"optShapeMapFormatFile: $optShapeMapFormatFile")
      TriggerModeParam(
        optTriggerMode,
        optShapeMap,
        optShapeMapFormatTextArea,
        optShapeMapURL,
        optShapeMapFormatUrl,
        optShapeMapFile,
        optShapeMapFormatFile,
        optActiveShapeMapTab
      )
    }
    val r: IO[Either[String, TriggerModeParam]] = tp.map(_.asRight[String])
    r.flatMap(
      _.fold(
        str =>
          IO.raiseError(
            new RuntimeException(s"Error obtaining validation trigger: $str")
          ),
        IO.pure
      )
    )
  }
}
