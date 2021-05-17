package es.weso.rdfshape.server.server

import cats.effect.IO
import cats.implicits._
import es.weso.rdf.PrefixMap
import es.weso.shapemaps._
import org.log4s.getLogger

case class ShapeMapParam(
    shapeMap: Option[String],
    shapeMapURL: Option[String],
    shapeMapFile: Option[String],
    optShapeMapFormat: Option[ShapeMapFormat],
    targetShapeMapFormat: Option[ShapeMapFormat],
    activeShapeMapTab: Option[String]
) {

  val shapeMapFormat: String = optShapeMapFormat.getOrElse(Compact).name
  val shapeMapTab: String    = activeShapeMapTab.getOrElse(ShapeMapTextAreaType.id)
  private[this] val logger = getLogger

  def getShapeMap: IO[ShapeMap] =
    for {
      tab <- IO.fromEither(
        parseShapeMapTab(shapeMapTab).leftMap(e => new RuntimeException(e))
      )
      sm <- tab match {
        case ShapeMapTextAreaType =>
          IO.fromEither(
            ShapeMap
              .fromString(shapeMap.getOrElse(""), shapeMapFormat)
              .leftMap(es => new RuntimeException(es.toList.mkString("\n")))
          )
        case ShapeMapUrlType =>
          for {
            e <- ShapeMap.fromURI(
              shapeMapURL.getOrElse(""),
              shapeMapFormat,
              None,
              PrefixMap.empty,
              PrefixMap.empty
            )
            r <- e.fold(
              ls =>
                IO.raiseError(new RuntimeException(ls.toList.mkString("\n"))),
              IO.pure
            )
          } yield r
        case _ =>
          IO.raiseError(new RuntimeException(s"Not implemented yet ${tab.id}"))
      }
    } yield sm

  def parseShapeMapTab(tab: String): Either[String, ShapeMapInputType] = {
    logger.debug(s"parseShapeMapTab: tab = $tab")
    val inputTypes =
      List(ShapeMapUrlType, ShapeMapFileType, ShapeMapTextAreaType)
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

  case object ShapeMapUrlType extends ShapeMapInputType {
    override val id = "#shapeMapUrl"
  }

  case object ShapeMapFileType extends ShapeMapInputType {
    override val id = "#shapeMapFile"
  }

  case object ShapeMapTextAreaType extends ShapeMapInputType {
    override val id = "#shapeMapTextArea"
  }

}

object ShapeMapParam {
  private[this] val logger = getLogger

  private[server] def mkShapeMap(
      partsMap: PartsMap
  ): IO[(ShapeMap, ShapeMapParam)] =
    for {
      smp <- mkShapeMapParam(partsMap)
      sm  <- smp.getShapeMap
    } yield (sm, smp)

  private[server] def mkShapeMapParam(partsMap: PartsMap): IO[ShapeMapParam] =
    for {
      shapeMap       <- partsMap.optPartValue("shapeMap")
      shapeMapURL    <- partsMap.optPartValue("shapeMapURL")
      shapeMapFile   <- partsMap.optPartValue("shapeMapFile")
      shapeMapFormat <- getShapeMapFormat("shapeMapFormat", partsMap)
      targetShapeMapFormat <- getShapeMapFormat(
        "targetShapeMapFormat",
        partsMap
      )
      activeShapeMapTab <- partsMap.optPartValue("shapeMapActiveTab")
    } yield {
      ShapeMapParam(
        shapeMap,
        shapeMapURL,
        shapeMapFile,
        shapeMapFormat,
        targetShapeMapFormat,
        activeShapeMapTab
      )
    }

  private def getShapeMapFormat(
      name: String,
      partsMap: PartsMap
  ): IO[Option[ShapeMapFormat]] =
    for {
      maybeStr <- partsMap.optPartValue(name)
    } yield maybeStr match {
      case None => None
      case Some(str) =>
        ShapeMapFormat
          .fromString(str)
          .fold(
            err => {
              logger.error(s"Unsupported shapeMapFormat: $str")
              None
            },
            smf => Some(smf)
          )
    }

  private[server] def empty: ShapeMapParam =
    ShapeMapParam(None, None, None, None, None, None)

}
