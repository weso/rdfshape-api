package controllers
import org.specs2._

import play.api.test._
import play.api.test.Helpers._
import models._


class ApplicationSpec extends Specification { def is = 
  
  "Specification of Application" 							^
  															p^
  "The application should" 									^
  	"respond to plain index with html"						!e1^
  	"respond to search without parameters with BAD_REQUEST"	!e2^
  	"respond to parameterized search text/html" 			!e3^
  	"respond to parameterized search text/html and find persona" !e4^
  															end

  def e1 = {
	  val result = controllers.Application.index(FakeRequest())
  
	  status(result) must equalTo(OK)
	  contentType(result) must beSome("text/html")
	  charset(result) must beSome("utf-8")
	  contentAsString(result) must contain("Labels4all.info")  
   }
  
  def e2 = {
	  val result = controllers.Application.searchTranslation(FakeRequest())
  
	  status(result) must equalTo(BAD_REQUEST)
   }
  
  def e3 = {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val langCode = "es"
      val langName = "Spanish"
      Language.create(langCode,langName)
      val idLang = Language.lookup(langCode)
      
      val foafPerson = "http://xmlns.com/foaf/0.1/Person"
      IRI.create(foafPerson)
      val idIRI = IRI.lookup(foafPerson)
      
      Translation.create(idIRI.get,idLang.get,"Persona",1)


	  val Some(result) = routeAndCall(FakeRequest(GET, "/search?iriName=http%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2FPerson&langCode=es"))
	  status(result) must equalTo(OK)
	  contentType(result) must beSome("text/html")
    }
   }

 	def e4 = {  
    running(FakeApplication()) {
      Language.create("es","Spanish")
      Language.create("en","English")
      val es = Language.lookup("es")
      val en = Language.lookup("en")
      
      val foafPerson = "http://xmlns.com/foaf/0.1/Person"
      val foafProject = "http://xmlns.com/foaf/0.1/Project"
      IRI.create(foafPerson)
      IRI.create(foafProject)

      val person = IRI.lookup(foafPerson)
      
      Translation.create(person.get,es.get,"Humano",1)
      Translation.create(person.get,es.get,"Persona",2)
      Translation.create(person.get,es.get,"Paisanu",1)
      Translation.create(person.get,en.get,"Person",1)


	  val Some(result) = routeAndCall(FakeRequest(GET, "/search?iriName=http%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2FPerson&langCode=es"))
	  status(result) must equalTo(OK)
	  contentType(result) must beSome("text/html")
	  contentAsString(result) must contain("Persona")  
    }
   }

}