package es.weso.rdfshape

import cats.effect._
import com.typesafe.scalalogging._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.schema._
import es.weso.server._
import es.weso.utils.FileUtils
import org.rogach.scallop._
import org.rogach.scallop.exceptions._

import java.nio.file._
import scala.concurrent.duration._

// import es.weso.quickstart.QuickStartMain
import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.IRI

object Main extends App with LazyLogging {
  try {
    run(args)
  } catch {
    case e: Exception =>
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
