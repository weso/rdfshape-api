package es.weso.utils

import scala.util._


object CommonUtils {

 def getWithRecoverFunction[T](t : Try[T], recoverFunction: Throwable => T): T = {
    t match {
      case Success(v) => v
      case Failure(e) => recoverFunction(e)
    }
} 

}
