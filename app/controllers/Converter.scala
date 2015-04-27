package controllers

import play.api._
import play.api.mvc._
import es.weso.shex.Schema

object Converter extends Controller {

  def data = Action {
    Ok("RDF Data conversions not implemented yet")
  }

  def schema = Action {
    Ok("Schema conversions not implemented yet")
  }
  
}