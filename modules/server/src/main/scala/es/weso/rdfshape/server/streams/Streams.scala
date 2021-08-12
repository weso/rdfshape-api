package es.weso.rdfshape.server.streams

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.SPARQLQueries.queryTriplesWithSubject
import es.weso.rdf.nodes.IRI
import org.apache.commons.io.output.WriterOutputStream
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.system._
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.http4s.Uri

import java.io.{OutputStream, StringWriter}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

/** Utilities for working with RDF data and its extraction from remote source
  */
object Streams extends LazyLogging {

  /** @param uri URI to read from
    * @param lang Output RDF syntax (turtle, n-triples...)
    * @return Raw RDF data from a remote URI in plain text using the specified syntax
    */
  def getRdfRaw(
      uri: Uri,
      lang: Lang = Lang.TURTLE
  ): IO[String] = {

    getRdf(
      uri,
      lang,
      (stringWriter, _, rdfStream) => {
        IO {
          RDFDataMgr.parse(rdfStream, uri.renderString)
          val raw = stringWriter.toString
          logger.debug(s"Raw graph: $raw")
          raw
        }
      }
    )

  }

  /** Generic function for private use. Given an RDF-extracting function, executes it while checking for errors and closing all resources used in the process.
    *
    * @param uri         URI to read from
    * @param lang        Output RDF syntax (turtle, n-triples...)
    * @param getRdfLogic Logic in charge of extracting RDF from sources
    * @param encoding    Encoding with which the data extracted is stored
    * @return String representation of the RDF data extracted (in the specified language and encoding)
    */
  private def getRdf(
      uri: Uri,
      lang: Lang,
      getRdfLogic: (StringWriter, OutputStream, StreamRDF) => IO[String],
      encoding: Charset = UTF_8
  ): IO[String] = {

    /* Get the necessary elements (writer, streams, etc.) to read the RDF data
     * and store it in plain text if needed. */
    val streamsIOElements = StreamsIOElements(lang, encoding)
    val (stringWriter, outputStream, rdfStream) =
      StreamsIOElements.unapply(streamsIOElements)

    /* Extract the String representation of the URI and pick up the data from
     * the initial StringWriter.
     * DATA => StreamRDF => OutputStream => StringWriter */
    try {
      getRdfLogic(stringWriter, outputStream, rdfStream)
    } catch {
      // Log errors before throwing
      case e: Throwable =>
        logger.error(s"Error parsing RDF data from $uri: ${e.getMessage}")
        throw e
    } finally {
      // Always close the output stream
      outputStream.close()
    }

  }

  /** @param uri URI to read from
    * @param lang Output RDF syntax (turtle, n-triples...)
    * @return Graphed RDF data from a remote URI in plain text using the specified syntax
    */
  def getRdfRawWithModel(uri: Uri, lang: Lang = Lang.NTRIPLES): IO[String] = {
    getRdf(
      uri,
      lang,
      (stringWriter, _, rdfStream) => {
        IO {
          val model       = ModelFactory.createDefaultModel
          val modelGraph  = model.getGraph
          val streamGraph = StreamRDFLib.graph(modelGraph)
          RDFDataMgr.parse(streamGraph, uri.renderString)

          logger.debug(s"Model graph: $model")

          StreamRDFOps.sendGraphToStream(modelGraph, rdfStream)
          stringWriter.toString
        }
      }
    )

  }

  /** @param endpoint URI to read from
    * @param node      Node to query the endpoint about
    * @param lang      Output RDF syntax (turtle, n-triples...)
    * @return Outgoing node information in RDF from a remote endpoint in plain text with the specified syntax
    */
  def getOutgoing(
      endpoint: Uri,
      node: String,
      lang: Lang = Lang.TURTLE
  ): IO[String] =
    getRdf(
      endpoint,
      lang,
      (stringWriter, _, rdfStream) => {
        IO {
          val query = QueryExecutionFactory
            .sparqlService(
              endpoint.renderString,
              queryTriplesWithSubject(IRI(node))
            )
            .execConstruct()

          val graph = query.getGraph
          logger.debug(s"Query graph: $graph")

          StreamRDFOps.sendGraphToStream(graph, rdfStream)
          stringWriter.toString

        }
      }
    )

}

/** Data class used as a factory for the repetitive task of instantiating
  * the IO tools (StringWriters, OutputStreams, RDFStreams) used for RDF reading, parsing and storing
  *
  * @param stringWriter String buffer used to store RDF data in plain text
  * @param outputStream OutputStream receiving RDF data and sending it to the writer
  * @param streamRDF    RDFStream used for reading RDF data and sending it to the OutputStream once formatted
  */
sealed case class StreamsIOElements(
    stringWriter: StringWriter,
    outputStream: OutputStream,
    streamRDF: StreamRDF
)

object StreamsIOElements {

  /** Factory method
    *
    * @param lang     Syntax that the RDFStream with use to output RDF data
    * @param encoding Encoding that the OutputStream will use to output data
    * @return A new data object with IO utils
    */
  def apply(
      lang: Lang = Lang.TURTLE,
      encoding: Charset = UTF_8
  ): StreamsIOElements = {
    // Create basic StringWriter and attach it to an OutputStream
    val stringWriter = new StringWriter
    val outputStream: OutputStream =
      new WriterOutputStream(stringWriter, encoding)
    // Create an RDF StreamWriter outputting to the previous OutputStream
    val rdfStream: StreamRDF =
      StreamRDFWriter.getWriterStream(outputStream, lang)

    new StreamsIOElements(stringWriter, outputStream, rdfStream)
  }

  /** @param it StreamsIOElements containing the IO utils to parse RDF
    * @return A tuple will the IO Utils ready to be destructured in other parts of the code
    */
  def unapply(
      it: StreamsIOElements
  ): (StringWriter, OutputStream, StreamRDF) = {
    (it.stringWriter, it.outputStream, it.streamRDF)
  }
}
