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

    val baseFolder: Path = if(opts.baseFolder.isDefined) {
      Paths.get(opts.baseFolder())
    } else {
      Paths.get(".")
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

  def printTime(msg: String, opts: MainOpts, nanos: Long): Unit = {
    if(opts.time()) {
      val time = Duration(nanos, NANOSECONDS).toMillis
      println(f"$msg%s, $time%10d")
    }
  }

  /* def getShapeMapStr(opts: MainOpts): IO[String] = { if
   * (opts.shapeMap.isDefined) { // val shapeMapFormat =
   * opts.shapeMapFormat.toOption.getOrElse("COMPACT") for { // TODO: Allow
   * different shapeMap formats content <-
   * FileUtils.getContents(opts.shapeMap()) } yield content.toString } else
   * EitherT.pure[IO,String]("") } */

  def getRDFReader(
      opts: MainOpts,
      baseFolder: Path
  ): IO[Resource[IO, RDFReader]] = {
    val base = Some(IRI(FileUtils.currentFolderURL))
    if(opts.data.isDefined) {
      val path = baseFolder.resolve(opts.data())
      for {
        res <- RDFAsJenaModel.fromFile(path.toFile, opts.dataFormat(), base)
        /* newRdf <- if (opts.inference.isDefined) {
         * io2es(rdf.applyInference(opts.inference())) } else ok_es(rdf) */
      } yield res
    } else {
      logger.info("RDF Data option not specified")
      RDFAsJenaModel.empty
    }
  }

  def getSchema(
      opts: MainOpts,
      baseFolder: Path,
      rdf: RDFReader
  ): IO[Schema] = {
    val base = Some(FileUtils.currentFolderURL)
    if(opts.schema.isDefined) {
      val path = baseFolder.resolve(opts.schema())
      val schema = Schemas.fromFile(
        path.toFile,
        opts.schemaFormat(),
        opts.engine(),
        base
      )
      schema
    } else {
      logger.info("Schema not specified. Extracting schema from data")
      Schemas.fromRDF(rdf, opts.engine())
    }
  }
}
