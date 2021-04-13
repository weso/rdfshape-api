package es.weso.server

import cats.effect._
import org.http4s._
import org.http4s.server.staticcontent.resourceServiceBuilder
// import cats.effect.IO._
import org.http4s.dsl.Http4sDsl
import org.log4s.getLogger

class StaticService() extends Http4sDsl[IO] {

  private val logger = getLogger

  def routes: HttpRoutes[IO] = {
    resourceServiceBuilder("/static").toRoutes
  } // .combineK(webjars)

  /* private val webjars: HttpRoutes[F] = { def isJsAsset(asset: WebjarAsset):
   * Boolean = asset.asset.endsWith(".js") webjarService( Config( filter =
   * isJsAsset, blocker = blocker ) ) } */
}

object StaticService {
  def apply(): StaticService = new StaticService()
}
