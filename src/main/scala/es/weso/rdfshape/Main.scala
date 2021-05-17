package es.weso.rdfshape

import com.typesafe.scalalogging._
import es.weso.rdfshape.cli.CliManager
import es.weso.rdfshape.server.launcher.Server

import scala.util.control.NonFatal

object Main extends App with LazyLogging {

  try {
    run(args)
  } catch {
    case NonFatal(e) =>
      println(s"Error while running the application: ${e.getMessage}")
  }

  def run(args: Array[String]): Unit = {
    // Parse and verify arguments
    val opts = new CliManager(args)

    val server  = opts.server.apply()
    val port    = opts.port.apply()
    val verbose = opts.verbose.apply()

    // Start the server module
    if(server) {
      CliManager.printBanner()
      Server(port, verbose)
    }
  }
}
