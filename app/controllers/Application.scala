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
    label => {
      IRI.create(label)
      Redirect(routes.Application.iris)
    }
   )
  }	  
 
 def deleteIRI(id: Long) = Action {
  Task.delete(id)
  Redirect(routes.Application.iris)
}
  
  

  val iriForm = Form(
  "IRI Name" -> nonEmptyText
  )
  
  import models.IRI

  def iris = Action {
	  Ok(views.html.index(IRI.all(), iriForm))
  }
}