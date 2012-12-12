package controllers
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class IndexSpec extends Specification {

  "render flash" in {
    implicit val flash = new play.api.mvc.Flash(Map(("message","My flash"))) 
	val html = views.html.index(Application.searchForm)
    contentType(html) must equalTo("text/html")
    contentAsString(html) must contain("My flash")
  }
}