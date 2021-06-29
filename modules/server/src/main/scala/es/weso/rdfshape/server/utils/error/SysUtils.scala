package es.weso.rdfshape.server.utils.error

/** Static utility methods user for system-wide error operations, i.e., forcibly exiting the system and keeping track of
  * all custom error codes
  */
object SysUtils {

  /** Exit code on successful program execution
    */
  val successCode = 0

  // CLI errors
  /** Exit code on CLI argument parsing error
    */
  val parseArgumentsError = 101

  /** Exit code on invalid CLI arguments
    */
  val invalidArgumentsError = 102

  // Server startup errors
  /** Exit code on runtime error when trying to build and SSL Context for the app
    * @see {@link es.weso.rdfshape.server.utils.secure.SSLHelper}
    */
  val sslContextCreationError = 201

  /** Terminates the program with a given error code after printing a given message in standard error
    * @param code Exit code of the program
    * @param message Message to be printed before exiting
    */
  def fatalError(code: Int, message: String): Unit = {
    System.err.println(message)
    sys.exit(code)
  }
}
