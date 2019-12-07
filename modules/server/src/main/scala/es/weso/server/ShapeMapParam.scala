package es.weso.server

import java.net.URI

import Defaults._
import cats.data.EitherT
import cats.implicits._
import cats.effect.{Effect, IO}
import es.weso.html2rdf.HTML2RDF
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena._
import es.weso.rdf.nodes.IRI
import es.weso.server.helper.DataFormat
import org.log4s.getLogger
import es.weso.shapeMaps._

case class ShapeMapParam(shapeMap: Option[String],
                     shapeMapURL: Option[String],
                     shapeMapFile: Option[String],
                     optShapeMapFormat: Option[ShapeMapFormat],
                     targetShapeMapFormat: Option[ShapeMapFormat],
                     activeShapeMapTab: Option[String]
                    ) {

  private[this] val logger = getLogger

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

  val shapeMapFormat = optShapeMapFormat.getOrElse(Compact).name
  val shapeMapTab = activeShapeMapTab.getOrElse(ShapeMapTextAreaType.id)

  def parseShapeMapTab(tab: String): Either[String, ShapeMapInputType] = {
    logger.debug(s"parseShapeMapTab: tab = $tab")
    val inputTypes = List(ShapeMapUrlType,ShapeMapFileType,ShapeMapTextAreaType)
    inputTypes.find(_.id == tab) match {
      case Some(x) => Right(x)
      case None => Left(s"Wrong value of tab: $tab, must be one of [${inputTypes.map(_.id).mkString(",")}]")
    }
  }

  def getShapeMap[F[_]: Effect]: EitherT[F, String, ShapeMap] = for {
    tab <- EitherT.fromEither[F](parseShapeMapTab(shapeMapTab))
    sm <- tab match {
      case ShapeMapTextAreaType => EitherT.fromEither[F](ShapeMap.fromString(shapeMap.getOrElse(""), shapeMapFormat))
      case _ => EitherT.fromEither[F](s"Not implemented yet ${tab.id}".asLeft[ShapeMap])
    }
  } yield sm

}

object ShapeMapParam {
  private[this] val logger = getLogger

  private[server] def mkShapeMap[F[_]:Effect](partsMap: PartsMap[F]): EitherT[F,String,(ShapeMap,ShapeMapParam)] = for {
    smp <- EitherT.liftF[F,String,ShapeMapParam](mkShapeMapParam(partsMap))
    sm <- smp.getShapeMap
  } yield ((sm,smp))

  private def getShapeMapFormat[F[_]](name: String, partsMap: PartsMap[F])(implicit F: Effect[F]): F[Option[ShapeMapFormat]] = for {
    maybeStr <- partsMap.optPartValue(name)
  } yield maybeStr match {
    case None => None
    case Some(str) => ShapeMapFormat.fromString(str).fold(
      err => {
        logger.error(s"Unsupported shapeMapFormat: $str")
        None
      },
      smf => Some(smf)
    )
  }

  private[server] def mkShapeMapParam[F[_]:Effect](partsMap: PartsMap[F]): F[ShapeMapParam] = for {
    shapeMap <- partsMap.optPartValue("shapeMap")
    shapeMapURL <- partsMap.optPartValue("shapeMapURL")
    shapeMapFile <- partsMap.optPartValue("shapeMapFile")
    shapeMapFormat <- getShapeMapFormat("shapeMapFormat", partsMap)
    targetShapeMapFormat <- getShapeMapFormat("targetShapeMapFormat",partsMap)
    activeShapeMapTab <- partsMap.optPartValue("shapeMapActiveTab")
  } yield {
    ShapeMapParam(shapeMap,shapeMapURL,shapeMapFile,shapeMapFormat,
      targetShapeMapFormat,activeShapeMapTab)
  }

  private[server] def empty: ShapeMapParam =
    ShapeMapParam(None,None,None,None,None,None)


}