package controllers

import play.api._
import play.api.mvc._
import es.weso.shex.Schema

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def about = Action {
    Ok(views.html.about(Schema.id))
  }
  
}