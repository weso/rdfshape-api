package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import models._
import play.api.i18n._
import anorm._
import views.html.defaultpages.badRequest

object Application extends Controller {
  
  var languages : Seq[Lang] = Seq()
    
  implicit val flash = new play.api.mvc.Flash(Map(("message",""))) 

  def index = Action { implicit request =>

    languages = request.acceptLanguages
    
    request match {
      case Accepts.Html() => Ok(views.html.index(languages,searchForm))
      case Accepts.Json() => Ok("JSON")
      case Accepts.Xml() => Ok("XML . Languages: " + languages)
      case _ => if (request.accepts("text/turtle")) Ok("Turtle")
                else Ok(views.html.index(languages,searchForm))
      
    }
  }

  def about = Action { implicit request =>
    Ok(views.html.about())
  }
  
  def home(flash : Flash) = views.html.index(languages,searchForm)(flash)

  def searchTranslation = Action { implicit request =>

    searchForm.bindFromRequest.fold(
    errors => Ok("Error " + errors.toString()), // BadRequest(views.html.index(Language.all(), errors)),
    searchField => {
      val result = Translation.lookupTranslation(searchField.iriName, searchField.langName)
      result match { 
        case None => 		NotFound
        case Some(trans) => 
              val flash = Flash(Map(("message",trans.transLabel)))
              request match {
              	case Accepts.Html() => Ok(home(flash))
              	case _ => if (request.accepts("text/turtle")) 
              				Ok("Turtle")
              			  else Ok(home(flash))
              }
      }
    }
   ) 
  }

   val searchForm : Form[SearchField] = Form (
     mapping(
      "iriName" -> nonEmptyText,
      "langCode" -> nonEmptyText
     )(SearchField.apply)(SearchField.unapply)
  )

}