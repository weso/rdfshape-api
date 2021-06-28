package es.weso.rdfshape

import ch.qos.logback.classic.util.ContextInitializer
import com.typesafe.scalalogging._
import es.weso.rdfshape.cli.CliManager
import es.weso.rdfshape.server.Server

object Main extends App with LazyLogging {

  run(args)

  private def run(args: Array[String]): Unit = {
    System.setProperty(
      ContextInitializer.CONFIG_FILE_PROPERTY,
      "logback-configurations/logback.groovy"
    )

    // Parse and verify arguments
    val opts = new CliManager(args)

    val port    = opts.port.apply()
    val verbose = opts.verbose.apply()
    val https   = opts.https.apply()

    // Start the server module
    CliManager.printBanner()
    println("CLI arguments parsed...")
    Server(port, https, verbose)
  }
}
