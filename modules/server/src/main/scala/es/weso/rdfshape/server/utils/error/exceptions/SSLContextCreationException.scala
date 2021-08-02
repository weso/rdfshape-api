package es.weso.rdfshape.server.utils.error.exceptions

/** Custom exception thrown when a failure occurs when trying to create an SSL Context from user's environment data
  *
  * @param message Reason/explanation of why the exception occurred
  * @param cause   Nested exception that caused the SSL Context creation to fail
  */
final case class SSLContextCreationException(
    private val message: String,
    private val cause: Throwable
) extends Exception(message, cause) {}

object SSLContextCreationException {

  /** Fixed message preceding the exception message
    */
  private val prefix =
    "Could not create an SSL context with the specified configuration: "

  /** Factory method used for instantiating {@linkplain es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException}
    *
    * @param message Message of the new exception
    * @param cause Cause of the new exception
    * @return A new {@linkplain es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException SSLContextCreationException} with the given data.
    */
  def apply(
      message: String,
      cause: Throwable = None.orNull
  ): SSLContextCreationException = {
    cause match {
      case cause: SSLContextCreationException => cause
      case _                                  => new SSLContextCreationException(s"$prefix$message", cause)
    }
  }
}
