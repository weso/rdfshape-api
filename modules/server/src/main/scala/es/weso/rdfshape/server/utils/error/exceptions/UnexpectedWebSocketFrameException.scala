package es.weso.rdfshape.server.utils.error.exceptions

/** Custom exception thrown when a failure occurs because certain information
  * was expected via WebSockets but other arrived instead
  *
  * @param message Reason/explanation of why the exception occurred
  */
final case class UnexpectedWebSocketFrameException(
    private val message: String
) extends RuntimeException(message)

object UnexpectedWebSocketFrameException {

  /** Factory of [[UnexpectedWebSocketFrameException]]
    *
    * @param received Optionally, the received frame that caused the exception
    * @param expected Optionally, the expected class of the received frame
    * @return A new [[UnexpectedWebSocketFrameException]] with a customized
    *         error message
    */
  def apply[A, B](
      received: Option[Class[A]],
      expected: Option[Class[B]]
  ): UnexpectedWebSocketFrameException = {
    // Format the final messages to adapt to the cases where options are empty
    val formattedList = List(received, expected).map(
      _.map(_.getSimpleName).getOrElse("unspecified")
    )
    val (expectedText, receivedText) = (formattedList.head, formattedList(1))

    new UnexpectedWebSocketFrameException(
      s"Expected a WebSocket frame '$expectedText' but got '$receivedText'"
    )
  }
}
