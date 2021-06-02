package es.weso.rdfshape.server.streams
import cats.effect.IO
import es.weso.rdf.jena.SPARQLQueries.queryTriplesWithSubject
import es.weso.rdf.nodes.IRI
import org.apache.commons.io.output.WriterOutputStream
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.system._
import org.apache.jena.riot.{Lang, RDFDataMgr}

import java.io.{OutputStream, StringWriter}
import java.nio.charset.StandardCharsets.UTF_8
/* import org.apache.jena.riot.system.{StreamOps, StreamRDF, StreamRDFLib,
 * StreamRDFWriter, StreamRDFWriterFactory} */
import org.http4s.Uri

object Streams {

  def getRaw(uri: Uri): IO[String] = {
    val stringWriter     = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter, UTF_8)
    val destination: StreamRDF =
      StreamRDFWriter.getWriterStream(os, Lang.TURTLE)
    IO {
      RDFDataMgr.parse(destination, uri.renderString)
      stringWriter.toString
    }
  }

  def getRawWithModel(uri: Uri): IO[String] = {
    val stringWriter     = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter, UTF_8)
    val destination: StreamRDF =
      StreamRDFWriter.getWriterStream(os, Lang.NTRIPLES)
    IO {
      val model       = ModelFactory.createDefaultModel
      val modelGraph  = model.getGraph
      val streamGraph = StreamRDFLib.graph(modelGraph)
      RDFDataMgr.parse(streamGraph, uri.renderString)
      println(s"Model graph: ${model}")
      StreamRDFOps.sendGraphToStream(modelGraph, destination)
      stringWriter.toString
    }
  }

  def getOutgoing(endpoint: String, node: String): IO[String] = IO {
    println(s"Outgoing: $node at $endpoint")
    val c = QueryExecutionFactory
      .sparqlService(endpoint, queryTriplesWithSubject(IRI(node)))
      .execConstruct()
    val stringWriter     = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter, UTF_8)
    val destination: StreamRDF =
      StreamRDFWriter.getWriterStream(os, Lang.TURTLE)
    StreamRDFOps.sendGraphToStream(c.getGraph, destination)
    stringWriter.toString
  }

  /* The following code doesn't work...it raises premature EOF def
   * cnv[F[_]:ConcurrentEffect](stream: Stream[F,Byte]): Stream[F,String] = for
   * { is <- stream.through(toInputStream) str <- getRDF(is) } yield str
   *
   * def getRDF[F[_]:ConcurrentEffect](is: InputStream): Stream[F, String] = {
   * val stringWriter = new StringWriter val os: OutputStream = new
   * WriterOutputStream(stringWriter,UTF_8) val destination: StreamRDF =
   * StreamRDFWriter.getWriterStream(os,Lang.TURTLE) // val destination:
   * StreamRDF = StreamRDFLib.writer(writer) println(s"Before
   * RDFDataMgr.parse....") RDFDataMgr.parse(destination, is, Lang.TURTLE)
   * println(s"After parsing....") Stream.emit(stringWriter.toString) } */

}
