package es.weso.rdfshape.server.utils.error

/** Enum classifying the accepted exit codes by their Int code.
  */
case object ExitCodes {
  type ExitCodes = Int

  /** Exit code on successful program execution */
  val SUCCESS = 0
  // CLI ERRORS
  /** Exit code on CLI argument parsing error
    */
  val ARGUMENTS_PARSE_ERROR = 101

  /** Exit code on invalid CLI arguments
    */
  val ARGUMENTS_INVALID_ERROR = 102

  // Server startup errors
  /** Exit code on runtime error when trying to build and SSL Context for the app
    *
    * @see {@link es.weso.rdfshape.server.utils.secure.SSLHelper}
    */
  val SSL_CONTEXT_CREATION_ERROR = 201
}
