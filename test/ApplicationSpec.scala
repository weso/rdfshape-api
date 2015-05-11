import org.junit.runner._

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


class ApplicationSpec 
      extends PlaySpec 
      with OneAppPerSuite {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) mustBe None
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) mustBe (OK)
      contentAsString(home) must include("RDFShape")
    }
  }
}
