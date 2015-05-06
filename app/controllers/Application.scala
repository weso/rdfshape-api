package controllers

import play.api._
import play.api.mvc._
import es.weso.shex.Schema
import buildinfo._
// import rdfBuildinfo.{ BuildInfo => RDFBuildInfo }

object Application extends Controller {
  
  lazy val name = "RDFShape" 
  lazy val shexcalaName = BuildInfo.name + "(" + BuildInfo.version + ")" 

  def index = Action {
    Ok(views.html.index())
  }

  def about = Action {
    Ok(views.html.about(name + "|" + shexcalaName)(ValidationForm()))
  }

  def help = Action {
    Ok(views.html.help()(ValidationForm()))
  }
  
}