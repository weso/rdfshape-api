package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._

object Application extends Controller {
  
  def index = Action {
	  Redirect(routes.Application.iris)
  }
  
  def newIRI = Action { implicit request =>
  	iriForm.bindFromRequest.fold(
    errors => BadRequest(views.html.index(IRI.all(), errors)),
    iriName => {
      IRI.create(iriName)
      Redirect(routes.Application.iris)
    }
   )
  }	  
 
  def newLang = Action { implicit request =>
/*  	langForm.bindFromRequest.fold(
    errors => BadRequest(views.html.index(Lang.all(), errors)),
    langName => {
      Lang.create(langName)
      Redirect(routes.Application.langs)
    }
   )  */
    Ok("new Language")
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
    Ok("new Trans")
  }

  def deleteIRI(id: Long) = Action {
	  IRI.delete(id)
	  Redirect(routes.Application.index)
  }

 def deleteLang(id: Long) = Action {
  Language.delete(id)
  Ok("Deleted lang" + id)
//   Redirect(routes.Application.langs)
}

 def deleteTrans(id: Long) = Action {
  Translation.delete(id)
  Ok("Deleted trans" + id)
//  Redirect(routes.Application.trans)
}

  val iriForm = Form(
  "iriName" -> nonEmptyText
  )

  val langForm = Form(
		  tuple(
				  "langCode" -> text,
				  "langName" -> text
	 )  
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
	  Ok("Showing translations")
  }

}