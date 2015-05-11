import org.scalatest._
import org.scalatest.matchers._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import scala.collection.mutable.Stack
import play.api.mvc._
import scala.concurrent.Future
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

class IntegrationSpec 
  extends PlaySpec {

  "Application" should {

    "work from within a browser" in new WithBrowser {
      browser.goTo("http://localhost:" + port)
      browser.pageSource must include("RDFShape")
    }
    
  }
}
