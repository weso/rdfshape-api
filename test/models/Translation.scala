package models
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._


class TranslationSpec extends Specification {

  "create a translation" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val langCode = "es"
      val langName = "Spanish"
      Language.create(langCode,langName)
      val idLang = Language.lookup(langCode)
      
      val foafPerson = "http://xmlns.com/foaf/0.1/Person"
      IRI.create(foafPerson)
      val idIRI = IRI.lookup(foafPerson)
      
      Translation.create(idIRI.get,idLang.get,"Persona",1)

      val idt = Translation.lookupIds(idIRI.get,idLang.get)
	  idt must beSome
	  val t = Translation.findById(idt.get)
	  t must beSome
	  t.get.transLabel must beEqualTo("Persona")
    }
   }


}