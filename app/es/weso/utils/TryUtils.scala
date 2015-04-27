package es.weso.utils

import scala.util.{Try, Success, Failure}


object TryUtils {

 def recover[T](t : Try[T], recoverFunction: Throwable => T): T = {
    t match {
      case Success(_) => t.get
      case Failure(e) => recoverFunction(e)
    }
} 

}
