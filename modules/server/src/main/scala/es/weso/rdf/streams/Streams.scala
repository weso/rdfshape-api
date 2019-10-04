package es.weso.rdf.streams
import java.io.{InputStream, OutputStream, StringWriter}

import cats.effect.{ConcurrentEffect, IO, LiftIO}
import es.weso.rdf.jena.{Endpoint, RDFAsJenaModel}
import fs2._
import fs2.io._
import java.nio.charset.StandardCharsets.UTF_8

import es.weso.rdf.jena.SPARQLQueries.queryTriplesWithSubject
import es.weso.rdf.nodes.IRI
import org.apache.commons.io.output.WriterOutputStream
import org.apache.jena.graph.Graph
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.jena.riot.lang.PipedRDFStream
import org.apache.jena.riot.system._
// import org.apache.jena.riot.system.{StreamOps, StreamRDF, StreamRDFLib, StreamRDFWriter, StreamRDFWriterFactory}
import org.http4s.Uri
import org.opengis.metadata.identification.CharacterSet

object Streams {

  def getRaw[F[_]:ConcurrentEffect: LiftIO](uri: Uri):F[String] = {
    val stringWriter = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter,UTF_8)
    val destination: StreamRDF = StreamRDFWriter.getWriterStream(os,Lang.TURTLE)
    LiftIO[F].liftIO( IO {
      RDFDataMgr.parse(destination, uri.renderString)
      stringWriter.toString
    })
  }

  def getRawWithModel[F[_]:ConcurrentEffect: LiftIO](uri: Uri):F[String] = {
    val stringWriter = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter,UTF_8)
    val destination: StreamRDF = StreamRDFWriter.getWriterStream(os,Lang.NTRIPLES)
    LiftIO[F].liftIO( IO {
      val model = ModelFactory.createDefaultModel
      val modelGraph = model.getGraph
      val streamGraph = StreamRDFLib.graph(modelGraph)
      RDFDataMgr.parse(streamGraph, uri.renderString)
      println(s"Model graph: ${model}")
      StreamRDFOps.sendGraphToStream(modelGraph,destination)
      stringWriter.toString
    })
  }

  def getOutgoing[F[_]: LiftIO](endpoint: String, node: String): F[String] = LiftIO[F].liftIO( IO {
    val c = QueryExecutionFactory.sparqlService(endpoint, queryTriplesWithSubject(IRI(node))).execConstruct()
    val stringWriter = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter,UTF_8)
    val destination: StreamRDF = StreamRDFWriter.getWriterStream(os,Lang.TURTLE)
    StreamRDFOps.sendGraphToStream(c.getGraph,destination)
    stringWriter.toString
  })

/* The following code doesn't work...it raises premature EOF
  def cnv[F[_]:ConcurrentEffect](stream: Stream[F,Byte]): Stream[F,String] = for {
    is <- stream.through(toInputStream)
    str <- getRDF(is)
  } yield str

  def getRDF[F[_]:ConcurrentEffect](is: InputStream): Stream[F, String] = {
    val stringWriter = new StringWriter
    val os: OutputStream = new WriterOutputStream(stringWriter,UTF_8)
    val destination: StreamRDF = StreamRDFWriter.getWriterStream(os,Lang.TURTLE)
//    val destination: StreamRDF = StreamRDFLib.writer(writer)
    println(s"Before RDFDataMgr.parse....")
    RDFDataMgr.parse(destination, is, Lang.TURTLE)
    println(s"After parsing....")
    Stream.emit(stringWriter.toString)
  }
*/

}