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
  def transResult(viewTrans : ViewTranslation) = Ok(views.html.result(viewTrans,formatForm))
  
  def format = Action { implicit request =>
    formatForm.bindFromRequest.fold(
        errors => BadRequest("Error: " + errors.toString()),
        formatField => {
          val viewTrans = ViewTranslation(formatField.id,formatField.iriName,formatField.langName,formatField.label)
          if (request.accepts("text/turtle")) 
              prepareTurtle(viewTrans)
          else formatField.format match {
            case "text/turtle" => prepareTurtle(viewTrans)
            case "text/n3" => prepareTurtle(viewTrans)
            case "application/json" => prepareJson(viewTrans)
            case "application/xml" => prepareXML(viewTrans)
            case _ => Ok("Translation: " + viewTrans)
            
          }
          
        }
    )
  }

  def searchTranslation = Action { implicit request =>

    searchForm.bindFromRequest.fold(
    errors => BadRequest(views.html.index(languages,errors)),
    searchField => {
      val iriName = searchField.iriName
      val langName = searchField.langName
      val result = Translation.lookupTranslation(iriName, langName)
      result match { 
        case None => NotFound("Not found")
        case Some(trans) => 
              val viewTrans = ViewTranslation(trans.id.get,iriName,langName,trans.transLabel,trans.votes)
              contentNegotiation(request) match {
                case HTML() => transResult(viewTrans)
                case TURTLE() => prepareTurtle(viewTrans)
                case JSON() => prepareJson(viewTrans)
              }
      }
    }
   ) 
  }

  sealed class Format
  case class HTML() extends Format
  case class TURTLE() extends Format
  case class JSON() extends Format
  
  def contentNegotiation(request: RequestHeader, format: String = "") : Format = {
    format match {
      case "text/turtle" => TURTLE()
      case "text/n3" => TURTLE()
      case "text/html" => HTML()
      case "application/json" => JSON()
      case "" => 
      	request match {
      		case Accepts.Html() => HTML()
      		case Accepts.Json() => JSON()
      		case _ => {
      			request.headers.get("accept") match {
      				case None => HTML() // by default
      				case Some("text/turtle") => TURTLE()
      				case Some(_) => HTML()
      			}
      		}
      	} 
    }
  }
  
  val rdfslabel = "http://www.w3.org/2000/01/rdf-schema#label" 
    
  def prepareTurtle(viewTrans : ViewTranslation) = {
    Ok ("<" + viewTrans.iri + "> <" + rdfslabel + "> \"" + viewTrans.label + "\"@" + viewTrans.langCode + " ." )
  }
   
  def prepareJson(viewTrans : ViewTranslation) = {
    Ok ("{ iri: " + viewTrans.iri + "," +
         " label: " + viewTrans.label + "," +
         " language: " + viewTrans.langCode + 
        " }" )
  }
  def prepareXML(viewTrans : ViewTranslation) = {
    Ok ("<translation>\n" +
        " <iri>" + viewTrans.iri + "</iri>\n" +
        " <label>" + viewTrans.label + "</label>\n" +
        " <language>" + viewTrans.langCode + "</language>\n" +
        "</translation>" )
  }

  val formatForm : Form[FormatField] = Form (
      mapping(
      "id" -> of[Long],
      "iriName" -> nonEmptyText,
      "langCode" -> nonEmptyText,
      "label" -> nonEmptyText,
      "format" -> nonEmptyText,
      "votes" -> of[Long]
     )(FormatField.apply)(FormatField.unapply)
  )
  
  val searchForm : Form[SearchField] = Form (
     mapping(
      "iriName" -> nonEmptyText,
      "langCode" -> nonEmptyText
     )(SearchField.apply)(SearchField.unapply)
  )

}