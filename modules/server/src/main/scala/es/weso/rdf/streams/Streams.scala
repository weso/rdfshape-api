package es.weso.rdf.streams
import java.io.{InputStream, OutputStream, StringWriter}

import cats.effect.{ConcurrentEffect, IO, LiftIO}
import es.weso.rdf.jena.RDFAsJenaModel
import fs2._
import fs2.io._
import java.nio.charset.StandardCharsets.UTF_8

import org.apache.commons.io.output.WriterOutputStream
import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.jena.riot.lang.PipedRDFStream
import org.apache.jena.riot.system.{StreamOps, StreamRDF, StreamRDFLib, StreamRDFWriter, StreamRDFWriterFactory}
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

}