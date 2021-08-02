package es.weso.rdfshape.server.utils.error

import com.typesafe.scalalogging.LazyLogging

/** Static utility methods user for system-wide error operations, i.e., forcibly exiting the system and keeping track of
  * all custom error codes
  */
object SysUtils extends LazyLogging {

  /** Terminates the program with a given error code after logging a given error message
    *
    * @param code    Exit code of the program
    * @param message Message to be printed before exiting
    */
  def fatalError(code: Int, message: String): Unit = {
    logger.error(message)
    sys.exit(code)
  }
}
