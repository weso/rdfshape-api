package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.i18n._
import anorm._

object Application extends Controller {
  
  implicit val flash = new play.api.mvc.Flash(Map(("message",Messages("Greeting")(play.api.i18n.Lang("en"))))) 

  def Home = Ok(views.html.index())
  
  def index = Action { 
    Home
  }
  
  def newIRI = Action { implicit request =>
  	iriForm.bindFromRequest.fold(
    errors => Ok("Error " + errors.toString()),
              // TODO, // BadRequest(views.html.index()),
    iriName => {
      IRI.create(iriName)
      Redirect(routes.Application.iris)
//      Home.flashing("message" -> "IRI has been created")
    }
   )
  }	  
 
  def newLang = Action { implicit request =>
 	langForm.bindFromRequest.fold(
    errors => Ok("Error " + errors.toString()), // BadRequest(views.html.index(Language.all(), errors)),
    language => {
      Language.insert(language)
      Redirect(routes.Application.languages)
    }
   ) 
//    Home.flashing("message" -> "new Language")
  }
  
  def newTrans = Action { 
    /* implicit request =>
  	 transForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(Trans.all(), errors)),
        transLabel => {
        Trans.create(transLabel)
        Redirect(routes.Application.iris)
        }
     ) */
    Home.flashing("message" -> "new Translation")
  }

  def deleteIRI(id: Long) = Action {
	  IRI.delete(id)
	  Home.flashing("message" -> ("IRI " + id.toString + " deleted") )
  }

 def deleteLang(id: Long) = Action {
  Language.delete(Id(id))
  Redirect(routes.Application.languages)
}

 def deleteTrans(id: Long) = Action {
  Translation.delete(id)
  Ok("Deleted trans" + id)
//  Redirect(routes.Application.trans)
}

  val iriForm : Form[String] = Form(
  "iriName" -> nonEmptyText
  )

  
  val langForm : Form[Language] = Form(
     mapping(
      "id" -> ignored(NotAssigned:Pk[Long]),
      "langCode" -> text,
      "langName" -> text
     )(Language.apply)(Language.unapply)
  )

/*  val transForm = Form(
  "iriId" -> nonEmptyText, 
  "langId" -> nonEmptyText, 
  "transLabel" -> nonEmptyText
  ) */

  def iris = Action {
	  Ok(views.html.iris(IRI.all(), iriForm))
  }

  def languages = Action { 	  
    Ok(views.html.languages(Language.all(), langForm))
  }

  def translations = Action {
    Ok(views.html.translations(Translation.all()))
  }

}