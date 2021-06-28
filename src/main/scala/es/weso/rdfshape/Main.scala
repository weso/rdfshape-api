package es.weso.rdfshape

import ch.qos.logback.classic.util.ContextInitializer
import com.typesafe.scalalogging._
import es.weso.rdfshape.cli.{ArgumentsData, CliManager}
import es.weso.rdfshape.server.Server

object Main extends App with LazyLogging {

  run(args)

  private def run(args: Array[String]): Unit = {
    System.setProperty(
      ContextInitializer.CONFIG_FILE_PROPERTY,
      "logback-configurations/logback.groovy"
    )

    // Parse arguments
    val argumentsData = parseArguments(args)
    // Start the server module
    CliManager.printBanner()
    println("CLI arguments parsed...")
    Server(argumentsData.port, argumentsData.https, argumentsData.verbose)
  }

  /** Parse and validate the user-entered arguments
    * @param args Array of arguments passed to the executable
    * @return An {@linkplain es.weso.rdfshape.cli.ArgumentsData inmutable instance} that provides access to the arguments
    */
  private def parseArguments(args: Array[String]): ArgumentsData = {
    val cliManager = new CliManager(args)
    ArgumentsData(
      port = cliManager.port.apply(),
      https = cliManager.https.apply(),
      verbose = cliManager.verbose.apply()
    )
  }
}
