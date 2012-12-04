package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._

object Application extends Controller {
  
  def index = Action {
	  Redirect(routes.Application.tasks)
  }
  
  def newTask = Action { implicit request =>
  	taskForm.bindFromRequest.fold(
    errors => BadRequest(views.html.index(Task.all(), errors)),
    label => {
      Task.create(label)
      Redirect(routes.Application.tasks)
    }
   )
  }	  
 
 def deleteTask(id: Long) = Action {
  Task.delete(id)
  Redirect(routes.Application.tasks)
}
  
  

  val taskForm = Form(
  "label" -> nonEmptyText
  )
  
  import models.Task

  def tasks = Action {
	  Ok(views.html.index(Task.all(), taskForm))
  }
}