package es.weso.rdfshape

import com.typesafe.scalalogging._
import es.weso.launcher.Server

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
    val opts = new CLIOptions(args)

    val server  = opts.server.apply()
    val port    = opts.port.apply()
    val verbose = opts.verbose.apply()

    // Start the server submodule
    if(server) {
      CLIOptions.printBanner()
      Server.main(args)
    }
  }
}
