package controllers 

import org.scalatest._
import org.scalatest.matchers._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import scala.collection.mutable.Stack
import play.api.mvc._
import scala.concurrent.Future

class ApplicationTest 
  extends PlaySpec 
  with Results 
  with OneAppPerSuite {
  
  class ApplicationController extends Controller with Application
  
  "Application#index" should {
    "status must be ok" in {
      val controller = new ApplicationController()
      val result = controller.index().apply(FakeRequest())
      status(result) mustEqual OK    
      }

    "body must contain RDFShape" in {
      val controller = new ApplicationController()
      val result : Future[Result] = controller.index().apply(FakeRequest())
      val bodyText : String = contentAsString(result)
      bodyText must include("RDFShape")
    }

  }
  
    "Application#about" should {
     "status must be ok" in {
       val controller = new ApplicationController()
       val result = controller.about().apply(FakeRequest())
       status(result) mustEqual OK    
       }
     }
}