package models
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import anorm._

class LanguageSpec extends Specification {

  "create Language" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val langCode = "es"
      val langName = "Spanish"
      Language.create(langCode,langName)
      val id = Language.lookup(langCode)
	  id must beSome  
    }
   }
  
  "create the same language several times" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val langCode = "es"
      val langName = "Spanish"
      Language.create(langCode,langName)
      Language.create(langCode,langName)
      Language.create(langCode,langName)
      val id = Language.lookup(langCode)
	  id must beSome  
    }
   }

    "delete Language" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      Language.create("es","Español")
      Language.create("en", "English")

      val id = Language.lookup("es")
      id must beSome  
      Language.delete(Id(id.get))
      val id2 = Language.lookup("es")
      id2 must beNone
    }
   }

}