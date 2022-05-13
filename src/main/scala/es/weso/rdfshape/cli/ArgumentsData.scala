package es.weso.rdfshape.cli

/** Data class encapsulating the values read from the arguments passed to the application
  * @param port Port number read from CLI
  * @param https HTTPS value read from CLI (true or false)
  * @param verbosity Verbosity level read from CLI
  */
sealed case class ArgumentsData(
    port: Int,
    https: Boolean,
    verbosity: Int,
    silent: Boolean,
    streamTimeout: Int
)

object ArgumentsData {
  def unapply(
      argumentsData: ArgumentsData
  ): (Int, Boolean, Int, Boolean, Int) = {
    (
      argumentsData.port,
      argumentsData.https,
      argumentsData.verbosity,
      argumentsData.silent,
      argumentsData.streamTimeout
    )
  }
}
