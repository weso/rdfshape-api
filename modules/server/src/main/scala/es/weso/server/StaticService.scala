package es.weso.server

import cats.effect._
import es.weso._
import es.weso.schema._
import es.weso.server.QueryParams._
import org.http4s._
import org.http4s.multipart._
import org.http4s.twirl._
// import cats.effect.IO._
import cats.data.EitherT
import cats.implicits._
import es.weso.server.ApiHelper._
import es.weso.server.Defaults._
import es.weso.server.helper.DataFormat
import io.circe.Json
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}
import org.http4s.server.staticcontent.{ResourceService, resourceService, webjarService}
import org.log4s.getLogger

class StaticService() extends Http4sDsl[IO] {

  private val logger = getLogger

  def routes: HttpRoutes[IO] =
    resourceService[IO](ResourceService.Config("/static")) // .combineK(webjars)

/*  private val webjars: HttpRoutes[F] = {
    def isJsAsset(asset: WebjarAsset): Boolean = asset.asset.endsWith(".js")
    webjarService(
      Config(
        filter = isJsAsset,
        blocker = blocker
      )
    )
  } */
}

object StaticService {
  def apply(): StaticService = new StaticService()
}
