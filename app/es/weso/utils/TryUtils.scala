package es.weso.utils

import scala.util.{Try, Success => TrySuccess, Failure => TryFailure}


object TryUtils {

 def recover[T](t : Try[T], recoverFunction: Throwable => T): T = {
    t match {
      case TrySuccess(_) => t.get
      case TryFailure(e) => recoverFunction(e)
    }
} 

}
