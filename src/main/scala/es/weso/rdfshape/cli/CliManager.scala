package es.weso.rdfshape.cli

import es.weso.rdfshape.cli.CliManager.{
  formattedVersion,
  useHelpText,
  versionText
}
import es.weso.rdfshape.server.Server
import es.weso.rdfshape.server.utils.error.SysUtils
import org.rogach.scallop._
import org.rogach.scallop.exceptions.{Help, ValidationFailure, Version}

/**
 * Class in charge of parsing the arguments provided via command-line when executing RDFShape.
 * Parsed data is later used to instantiate the API server according to the user's needs.
 * @param arguments Array of arguments passed to the executable
 * @see es.weso.rdfshape.Main 
 */
class CliManager(arguments: Array[String]) extends ScallopConf(arguments) {

  // Configure the help menu
  version(formattedVersion)
  banner(
    s"""|USAGE: ${buildinfo.BuildInfo.name} [--port <port-number>] [--https] [--verbose]
        |${buildinfo.BuildInfo.name} is an mechanism for processing, validating and visualizing semantic data (RDF, SHEX, SHACL and more) through a REST API.
        |Options:
        |""".stripMargin
  )
  footer(s"\nFor further information, visit ${buildinfo.BuildInfo.apiURL.get}.")

  val port: ScallopOption[Int] = opt[Int](
    name = "port",
    short = 'p',
    default = Some(Server.defaultPort),
    required = false,
    validate = p => p > 0 && p <= 65535,
    descr =
      s"""Port in which the API will listen for requests. Values must be in range 1-65535 (defaults to ${Server.defaultPort})"""
  )

  val https: ScallopOption[Boolean] = opt[Boolean](
    name = "https",
    noshort = true,
    default = Some(Server.defaultHttps),
    required = true,
    descr =
      s"""Attempt to serve the API via HTTPS (defaults to ${Server.defaultHttps})"""
  )

  val verbose: ScallopOption[Int] = tally(
    name = "verbose",
    noshort = false,
    short = 'v',
    descr =
      s"""Show additional logging information (use cumulative times for additional info, like: "-vvv")"""
  )

  // Override the short forms of help and version arguments.
  val help: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr = s"""Print help menu and exit"""
  )
  val version: ScallopOption[Boolean] = opt[Boolean](
    noshort = true,
    descr =
      s"""Print the program version along with other related information and exit"""
  )

  override protected def onError(e: Throwable): Unit = e match {
    case Help("") =>
      printHelp()
      sys.exit(SysUtils.successCode)

    case Version =>
      println(versionText)
      sys.exit(SysUtils.successCode)

    case _: ValidationFailure =>
      SysUtils.fatalError(
        SysUtils.invalidArgumentsError,
        s"""
           |Invalid argument provided: ${e.getMessage}
           |$useHelpText
           |""".stripMargin
      )
    case _ =>
      SysUtils.fatalError(
        SysUtils.parseArgumentsError,
        s"""
          |Could not parse arguments: ${e.getMessage}
          |$useHelpText
          |""".stripMargin
      )
  }

  // Verify provided arguments
  verify()

}

object CliManager {
  private val useHelpText = s"""Use "--help" for usage information"""
  private lazy val formattedVersion: String =
    s"${buildinfo.BuildInfo.name} ${buildinfo.BuildInfo.version} by WESO Research Group (https://www.weso.es/)"
  private val versionText = s"Version: $formattedVersion"

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
    */
  def printBanner(): Unit = {
    println(bannerText)
  }
}
