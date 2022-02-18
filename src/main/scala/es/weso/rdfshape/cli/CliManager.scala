package es.weso.rdfshape.cli

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.cli.CliManager.{
  formattedVersion,
  useHelpText,
  versionText
}
import es.weso.rdfshape.server.Server
import es.weso.rdfshape.server.utils.error.{ExitCodes, SysUtils}
import org.rogach.scallop._
import org.rogach.scallop.exceptions.{Help, ValidationFailure, Version}

/** Class in charge of parsing the arguments provided via command-line when executing RDFShape.
  * Parsed data is later used to instantiate the API server according to the user's needs.
  *
  * @param arguments Array of arguments passed to the executable
  * @see es.weso.rdfshape.Main
  */
class CliManager(arguments: Array[String]) extends ScallopConf(arguments) {

  /** *
    * Set the program version text, as shown on the help menu.
    */
  version(formattedVersion)

  /** Set the informational banner text, as shown on the help menu
    */
  banner(
    s"""|USAGE: ${buildinfo.BuildInfo.name} [--port <port-number>] [--https] [--verbose]
        |${buildinfo.BuildInfo.name} is an mechanism for processing, validating and visualizing semantic data (RDF, SHEX, SHACL and more) through a REST API.
        |Options:
        |""".stripMargin
  )

  /** Set the footer text, as shown on the help menu
    */
  footer(s"\nFor further information, visit ${buildinfo.BuildInfo.apiURL.get}.")

  /** Configuration of the CLI argument setting the server's exposed port
    */
  val port: ScallopOption[Int] = opt[Int](
    name = "port",
    short = 'p',
    default = Some(Server.defaultPort),
    required = false,
    validate = p => p > 0 && p <= 65535,
    descr =
      s"""Port in which the API will listen for requests. Values must be in range 1-65535 (default is ${Server.defaultPort})"""
  )

  /** Configuration of the CLI argument setting whether the server should try to use HTTPS or not
    */
  val https: ScallopOption[Boolean] = opt[Boolean](
    name = "https",
    noshort = true,
    default = Some(Server.defaultHttps),
    required = true,
    descr =
      s"""Attempt to serve the API via HTTPS (default is ${Server.defaultHttps})"""
  )

  /** Configuration of the CLI argument setting the application's verbosity
    */
  val verbose: ScallopOption[Int] = tally(
    name = "verbose",
    noshort = false,
    short = 'v',
    descr =
      s"""Show additional logging information (use cumulative times for additional info, like: "-vvv")"""
  )

  /** Configuration of the CLI argument setting off all console output
    */
  val silent: ScallopOption[Boolean] = opt[Boolean](
    name = "silent",
    short = 's',
    default = Some(false),
    required = false,
    descr =
      s"""Enable silent mode in order not to log any output to console (default is ${false})"""
  )

  /** Configuration of the CLI argument triggering the application's help menu
    */
  // Override the short forms of help and version arguments.
  val help: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr = s"""Print help menu and exit"""
  )

  /** Configuration of the CLI argument triggering the application's version
    */
  val version: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr =
      s"""Print the program version along with other related information and exit"""
  )

  /** Error behaviour on wrong arguments or terminating arguments(those who do not launch
    * the app, e.g.; "version" and "help")
    */
  override protected def onError(e: Throwable): Unit = e match {
    // On "help", show help menu and exit
    case Help(_) =>
      printHelp()
      sys.exit(ExitCodes.SUCCESS)

    // On "version", show version and exit
    case Version =>
      println(versionText)
      sys.exit(ExitCodes.SUCCESS)

    // On args validation failure: exit, printing the specific error message
    case _: ValidationFailure =>
      SysUtils.fatalError(
        ExitCodes.ARGUMENTS_INVALID_ERROR,
        s"""
           |Invalid argument provided: ${e.getMessage}
           |$useHelpText
           |""".stripMargin
      )
    // On other CLI failure: exit, printing the specific error message
    case _ =>
      SysUtils.fatalError(
        ExitCodes.ARGUMENTS_PARSE_ERROR,
        s"""
           |Could not parse arguments: ${e.getMessage}
           |$useHelpText
           |""".stripMargin
      )
  }

  // Verify provided arguments
  verify()

}

/** Provide static members used by the CLI interface.
  *
  * @see {@link es.weso.rdfshape.cli.CliManager}
  */
object CliManager extends LazyLogging {

  /** Text message shown when showing the user the program version
    */
  private lazy val formattedVersion: String =
    s"${buildinfo.BuildInfo.name} ${buildinfo.BuildInfo.version} by WESO Research Group (https://www.weso.es/)"

  /** Text message shown when suggesting the user to check the help menu
    */
  private val useHelpText = s"""Use "--help" for usage information"""

  /** Simplified version text
    */
  private val versionText = s"Version: $formattedVersion"

  /** Formatted promotional banner shown on application startup
    */
  private val bannerText =
    """
      |
      |    ____  ____  ______   _____ __
      |   / __ \/ __ \/ ____/  / ___// /_  ____ _____  ___
      |  / /_/ / / / / /_      \__ \/ __ \/ __ `/ __ \/ _ \
      | / _, _/ /_/ / __/     ___/ / / / / /_/ / /_/ /  __/
      |/_/ |_/_____/_/       /____/_/ /_/\__,_/ .___/\___/
      |                                      /_/
      |
      |""".stripMargin

  /** Print a text-based banner with the program's name in a brand-like format
    * @note Using logger INFO level to not overload the console of users
    */
  def showBanner(): Unit = {
    logger.info(bannerText)
  }
}
