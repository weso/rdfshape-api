import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import play.Logger

object Global extends GlobalSettings {

  /* Default page if there is an error */
  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Exception: " + ex.getMessage)
    Future.successful(InternalServerError(
      views.html.errorPage(ex)
    ))
  }

}