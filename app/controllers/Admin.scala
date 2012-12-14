package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import models._
import play.api.i18n._
import anorm._

object Admin extends Controller {
  
  def admin = Action {
    Ok(views.html.admin())
  }
  
  def newIRI = Action { implicit request =>
  	iriForm.bindFromRequest.fold(
    errors => Ok("Errors" + errors), // BadRequest(views.html.index(searchForm)),
    iriName => {
      IRI.create(iriName)
      Redirect(routes.Admin.iris)
    }
   )
  }	  
 
  def newLang = Action { implicit request =>
 	langForm.bindFromRequest.fold(
    errors => Ok("Error " + errors.toString()), // BadRequest(views.html.index(Language.all(), errors)),
    language => {
      Language.insert(language)
      Redirect(routes.Admin.languages)
    }
   ) 
  }
  
   def newTranslation = Action { 
    implicit request =>
  	 translationForm.bindFromRequest.fold(
      errors => Ok("Error " + errors.toString()), // BadRequest(views.html.index(Trans.all(), errors)),
      vt => {
        Language.lookup(vt.langCode) match {
          case None => Ok("Language " + vt.langCode + " not found. Create language before")
          case Some(langId) =>
            IRI.lookup(vt.iri) match {
              case None => Ok("IRI " + vt.iri + " not found. Create IRI before")
              case Some(iriId) => 
                Translation.create(iriId,langId,vt.label,vt.votes)
                Redirect(routes.Admin.translations)
            }
        }
      }
   )
  }

  def deleteIRI(id: Long) = Action {
	  IRI.delete(Id(id))
	  Redirect(routes.Admin.languages)
      // Home.flashing("message" -> ("IRI " + id.toString + " deleted") )
  }

 def deleteLang(id: Long) = Action {
  Language.delete(Id(id))
  Redirect(routes.Admin.languages)
}

 def deleteTrans(id: Long) = Action {
  Translation.delete(Id(id))
  Ok("Deleted trans" + id)
  Redirect(routes.Admin.translations)
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

  val translationForm : Form[ViewTranslation] = Form(
     mapping(
      "id" -> of[Long],
      "iri" -> nonEmptyText,
      "langCode" -> nonEmptyText,
      "label" -> nonEmptyText,
      "votes" -> of[Long]
     )(ViewTranslation.apply)(ViewTranslation.unapply)
  )
  
   def iris = Action {
	  Ok(views.html.iris(IRI.all(), iriForm))
  }

  def languages = Action { 	  
    Ok(views.html.languages(Language.all(), langForm, Lang("es")))
  }

  def viewTranslations : List[ViewTranslation] = {
    Translation.all().map(t => ViewTranslation(t.id.get,
        IRI.findIRIName(t.iriId).getOrElse("Not found"), 
        Language.findLangCode(t.langId).getOrElse("Not found"),
        t.transLabel,
        t.votes.toInt)
    )
  }

  def translations = Action { request =>
    // TODO select default language
    Ok(views.html.translations(viewTranslations,translationForm,Lang("es")))
  }

  

 
}
