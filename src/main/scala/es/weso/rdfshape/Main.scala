package es.weso.rdfshape

import com.typesafe.scalalogging._
import es.weso.server._
import org.rogach.scallop._
import org.rogach.scallop.exceptions._
import scala.util.control.NonFatal

object Main extends App with LazyLogging {
  try {
    run(args)
  } catch {
    case NonFatal(e) =>
      println(s"Error: ${e.getMessage}")
  }

  def run(args: Array[String]): Unit = {
    val opts = new MainOpts(args, errorDriver)
    opts.verify()

    if(opts.server()) {
      RDFShapeServer.main(args)
    }
  }

  private def errorDriver(e: Throwable, scallop: Scallop) = e match {
    case Help(s) =>
      println("Help: " + s)
      scallop.printHelp
      sys.exit(0)
    case _ =>
      println("Error: %s".format(e.getMessage))
      scallop.printHelp
      sys.exit(1)
  }
}
