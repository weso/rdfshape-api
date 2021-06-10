package es.weso.rdfshape.server.utils.error.exceptions

final case class SSLContextCreationException(
    private val message: String,
    private val cause: Throwable
) extends Exception(message, cause) {}

object SSLContextCreationException {
  private val prefix =
    "Could not create an SSL context with the specified configuration: "

  def apply(
      message: String,
      cause: Throwable = None.orNull
  ): SSLContextCreationException = {
    cause match {
      case cause: SSLContextCreationException => cause
      case _                                  => new SSLContextCreationException(s"$prefix$message", cause)
    }
//    new SSLContextCreationException(s"$prefix$message", cause)
  }
}
