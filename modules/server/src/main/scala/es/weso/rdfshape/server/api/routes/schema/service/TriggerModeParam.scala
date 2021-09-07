package es.weso.rdfshape.server.api.routes.schema.service

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.definitions.ApiDefaults.defaultActiveShapeMapTab
import es.weso.rdfshape.server.api.format.ShapeMapFormat
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.shapemaps.ShapeMap

case class TriggerModeParam(
    triggerMode: Option[String],
    shapeMap: Option[String],
    shapeMapURL: Option[String],
    shapeMapFile: Option[String],
    shapeMapFormat: ShapeMapFormat,
    activeShapeMapTab: Option[String]
) extends LazyLogging {

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

            ShapeMap
              .fromURI(
                shapeMapUrl,
                shapeMapFormat.name,
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

            ShapeMap.fromString(shapeMapStr, shapeMapFormat.name, None) match {
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

            ShapeMap.fromString(shapeMapStr, shapeMapFormat.name, None) match {
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
      optTriggerMode  <- partsMap.optPartValue(TriggerModeParameter.name)
      optShapeMap     <- partsMap.optPartValue(ShapeMapTextParameter.name)
      optShapeMapURL  <- partsMap.optPartValue(ShapeMapUrlParameter.name)
      optShapeMapFile <- partsMap.optPartValue(ShapeMapFileParameter.name)

      shapeMapFormat <- getShapeMapFormat(
        ShapeMapFormatParameter.name,
        partsMap
      )
      optActiveShapeMapTab <- partsMap.optPartValue(
        ActiveShapeMapTabParameter.name
      )
    } yield {
      logger.debug(s"optTriggerMode: $optTriggerMode")
      logger.debug(s"optShapeMap: $optShapeMap")
      logger.debug(s"optActiveShapeMapTab: $optActiveShapeMapTab")
      logger.debug(s"optShapeMapFormat: $shapeMapFormat")
      TriggerModeParam(
        triggerMode = optTriggerMode,
        shapeMap = optShapeMap,
        shapeMapURL = optShapeMapURL,
        shapeMapFile = optShapeMapFile,
        shapeMapFormat = shapeMapFormat,
        activeShapeMapTab = optActiveShapeMapTab
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

  /** Try to build a {@link es.weso.rdfshape.server.api.format.ShapeMapFormat} object from a request's parameters
    *
    * @param parameter    Name of the parameter with the format name
    * @param parameterMap Request parameters
    * @return The ShapeMapFormat found or the default one
    */
  private def getShapeMapFormat(
      parameter: String,
      parameterMap: PartsMap
  ): IO[ShapeMapFormat] = {
    for {
      maybeFormat <- PartsMap.getFormat(parameter, parameterMap)
    } yield maybeFormat match {
      case None         => ShapeMapFormat.defaultFormat
      case Some(format) => new ShapeMapFormat(format)
    }

  }
}
