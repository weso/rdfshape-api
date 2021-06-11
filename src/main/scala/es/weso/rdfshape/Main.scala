package es.weso.rdfshape

import com.typesafe.scalalogging._
import es.weso.rdfshape.cli.CliManager
import es.weso.rdfshape.server.Server

object Main extends App with LazyLogging {

  run(args)

  def run(args: Array[String]): Unit = {
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
