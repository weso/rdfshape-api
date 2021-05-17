package es.weso.rdfshape

import org.rogach.scallop._
import org.rogach.scallop.exceptions.{Help, ValidationFailure, Version}

class CLIOptions(arguments: Array[String]) extends ScallopConf(arguments) {

  lazy val formattedVersion: String =
    s"${buildinfo.BuildInfo.name} ${buildinfo.BuildInfo.version} by WESO Research Group (https://www.weso.es/)"
  val defaultPort = 8080

  // Configure the help menu
  version(formattedVersion)
  banner(
    s"""|USAGE: ${buildinfo.BuildInfo.name} --port <port-number>
        |${buildinfo.BuildInfo.name} is an mechanism for processing, validating and visualizing semantic data (RDF, SHEX, SHACL and more) through a REST API.
        |Options:
        |""".stripMargin
  )
  footer(s"For further information, visit ${buildinfo.BuildInfo.apiURL.get}.")

  val server: ScallopOption[Boolean] = opt[Boolean](
    name = "server",
    short = 's',
    default = Some(true),
    descr =
      s"""Launch an HTTP server that will listen for request on the specified port (this option can be omitted)."""
  )

  val port: ScallopOption[Int] = opt[Int](
    name = "port",
    short = 'p',
    default = Some(defaultPort),
    required = false,
    validate = p => p > 0 && p <= 65535,
    descr =
      s"""Port in which the API will listen for requests. Values must be in range 1-65535 (defaults to $defaultPort)."""
  )

  val verbose: ScallopOption[Boolean] = opt[Boolean](
    name = "verbose",
    noshort = true,
    default = Some(false),
    descr = s"""Print the data received by the server as it is processed."""
  )

  // Override the short forms of help and version arguments.
  val help: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr = s"""Print help menu."""
  )
  val version: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr = s"""Print program version and related information."""
  )

  override protected def onError(e: Throwable): Unit = e match {
    case Help("") =>
      printHelp
      sys.exit(0)
    case Version =>
      println(s"Version: $formattedVersion")
      sys.exit(0)
    case _: ValidationFailure =>
      println(s"Invalid argument provided: ${e.getMessage}")
      println(s"""Use "--help" for usage information\n""")
      sys.exit(2)
    case _ =>
      println(s"""Could not parse arguments: ${e.getMessage}""")
      println(s"""Use "--help" for usage information\n""")
      sys.exit(1)
  }

  // Verify provided arguments
  verify()
}

object CLIOptions {
  private val asciiText: String =
    """
      |
      | __________________  _____ _
      || ___ \  _  \  ___| /  ___| |
      || |_/ / | | | |_    \ `--.| |__   __ _ _ __   ___
      ||    /| | | |  _|    `--. \ '_ \ / _` | '_ \ / _ \
      || |\ \| |/ /| |     /\__/ / | | | (_| | |_) |  __/
      |\_| \_|___/ \_|     \____/|_| |_|\__,_| .__/ \___|
      |                                      | |
      |                                      |_|
      |""".stripMargin

  def printBanner(): Unit = {
    println(asciiText)
  }
}
