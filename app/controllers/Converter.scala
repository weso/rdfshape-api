package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.concurrent.promise
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.ByteArrayInputStream
import org.apache.commons.io.FileUtils
import play.api._
import play.api.mvc._
import play.api.libs.Files._
import es.weso.shex.Schema
import scala.util._
import es.weso.rdf._
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.reader._
import es.weso.monads.{Result => SchemaResult, Failure => SchemaFailure}
import es.weso.shex._
import es.weso.monads._
import es.weso.utils._
import es.weso.utils.TryUtils._
import es.weso.utils.RDFUtils._
import java.net.URL
import java.io.File
import es.weso.utils.IOUtils._

object Converter extends Controller {

  def data = Action {
    Ok(views.html.convert_data());
  }

  def schema = Action {
    Ok("Schema conversions not implemented yet")
  }
  
  def convert_post = Action {
   NotImplemented("Not implemented conversions yet") 
  } 
  
}