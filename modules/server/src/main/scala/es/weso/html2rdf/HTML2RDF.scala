package es.weso.html2rdf
import java.io.ByteArrayOutputStream
import java.net.URI

import es.weso.rdf.{RDFReader, RDFReasoner}
import es.weso.rdf.jena.RDFAsJenaModel
import org.apache.any23.Any23
import org.apache.any23.extractor._
import org.apache.any23.extractor.microdata.MicrodataExtractor
import org.apache.any23.extractor.rdfa.{RDFa11Extractor, RDFa11ExtractorFactory}
import org.apache.any23.source.{HTTPDocumentSource, StringDocumentSource}
import org.apache.any23.writer._
import es.weso.server.utils.IOUtils._
import cats.effect.IO
import scala.util.Try
// import org.apache.jena.rdf.model._
import org.apache.jena.rdf.model.{
  Property => JenaProperty,
  RDFNode => JenaRDFNode,
  Resource => JenaResource,
  RDFReader => _,
  _}
import org.eclipse.rdf4j.model.{BNode, IRI, Literal, Resource, Value}

import scala.collection.JavaConverters._

object HTML2RDF {

  val availableExtractors =
    List(RDFA11, Microdata)

  val availableExtractorNames =
    availableExtractors.map(_.name)

  sealed trait Extractor {
    val name: String
  }
  case object RDFA11 extends Extractor {
    val extractor = new RDFa11Extractor()
    val name = extractor.getDescription.getExtractorName
  }
  case object Microdata extends Extractor {
    val extractor = new MicrodataExtractor
    val name = extractor.getDescription.getExtractorName
  }

  def extractFromString(htmlStr: String, extractorName: String): ESIO[RDFReasoner] = {
      Try {
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent");
      val httpClient = any23.getHTTPClient();
      val source     = new StringDocumentSource(htmlStr, "http://example.org")
      val handler = JenaTripleHandler(model)
      println("Initialization ready for extractor...")
      try {
        any23.extract(source, handler)
      } finally {
        handler.close
      }
      RDFAsJenaModel(model)
     }.fold(e => fail_es(e.getMessage()), ok_es(_))
  } 

  def extractFromUrl(uri: String, extractorName: String): ESIO[RDFReasoner] = {
    Try {
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent");
      val httpClient = any23.getHTTPClient();
      val source     = new HTTPDocumentSource(httpClient, uri)
      // val out = new ByteArrayOutputStream()
      val handler = JenaTripleHandler(model)
      try {
        any23.extract(source, handler)
      } finally {
        handler.close
      }
      // val n3: String = out.toString("UTF-8")
      println(s"Model: ${model}")
      RDFAsJenaModel(model)
    }.fold(e =>
      fail_es(s"Exception obtaining RDF from URI: ${e.getMessage}\nURI:\n$uri"),
      ok_es(_)
    )
  }

  case class JenaTripleHandler(m: Model) extends TripleHandler {

    override def receiveTriple(s: Resource,
                               p: IRI,
                               o: Value,
                               g: IRI,
                               context: ExtractionContext): Unit = {
      m.add(cnvSubj(s), cnvIRI(p), cnvObj(o))
      println(s"Triple: <${s},${p},${o}>")
    }
    override def startDocument(documentIRI: IRI): Unit = {} // println(s"Start")
    override def openContext(context: ExtractionContext): Unit = {} // println(s"New context")
    override def receiveNamespace(prefix: String,uri: String,context: ExtractionContext): Unit = {}
    override def closeContext(context: ExtractionContext): Unit     = {}
    override def endDocument(documentIRI: IRI): Unit = {}
    override def setContentLength(contentLength: Long): Unit = {}
    override def close(): Unit = {}

    def cnvSubj(r: Resource): JenaResource = r match {
      case i: IRI => cnvIRI(i)
      case b: BNode => cnvBNode(b)
    }

    def cnvIRI(p:IRI): JenaProperty =
      m.createProperty(p.toString)

    def cnvObj(o:Value): JenaRDFNode = o match {
      case i: IRI => cnvIRI(i)
      case b: BNode => cnvBNode(b)
      case l: Literal => if (l.getLanguage.isPresent) {
        m.createLiteral(l.getLabel,l.getLanguage.get)
      } else
        m.createTypedLiteral(l.getLabel, l.getDatatype.toString)
    }
    def cnvBNode(b: BNode): JenaResource =
      m.createResource(AnonId.create(b.getID))

  }
}