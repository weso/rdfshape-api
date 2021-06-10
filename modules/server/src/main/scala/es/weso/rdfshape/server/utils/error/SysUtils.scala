package es.weso.rdfshape.server.utils.error

object SysUtils {
  
  val successCode = 0

  // CLI errors
  val parseArgumentsError = 101
  val invalidArgumentsError = 102
 
  // Server startup errors
  val sslContextCreationError = 201

  /** Terminates the program with a given error code after printing a given message in standard error.
    * @param code Exit code of the program
    * @param message Message to be printed before exiting
    */
  def fatalError(code: Int, message: String): Unit = {
    System.err.println(message)
    sys.exit(code)
  }
}
