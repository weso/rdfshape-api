package es.weso.html2rdf
import java.io.ByteArrayOutputStream
import java.net.URI

import es.weso.rdf.{RDFReader, RDFReasoner}
import es.weso.rdf.nodes.IRI
import es.weso.rdf.jena.RDFAsJenaModel
import org.apache.any23.Any23
import org.apache.any23.extractor._
import org.apache.any23.extractor.microdata.MicrodataExtractor
import org.apache.any23.extractor.rdfa.{RDFa11Extractor, RDFa11ExtractorFactory}
import org.apache.any23.source.{HTTPDocumentSource, StringDocumentSource}
import org.apache.any23.writer._
import es.weso.utils.IOUtils._
import cats.effect.{Resource => CatsResource, _}
// import cats.effect.concurrent._
import scala.util.Try
// import org.apache.jena.rdf.model._
import org.apache.jena.rdf.model.{
  Property => JenaProperty,
  RDFNode => JenaRDFNode,
  Resource => JenaResource,
  RDFReader => _,
  _}
import org.eclipse.rdf4j.model.{BNode, IRI => RDF4jIRI, Literal, Resource, Value}

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

  def extractFromString(htmlStr: String, extractorName: String): CatsResource[IO,RDFReasoner] = {
      Try {
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent");
      val httpClient = any23.getHTTPClient();
      val source     = new StringDocumentSource(htmlStr, "http://example.org/")
      val handler = JenaTripleHandler(model)
      println("Initialization ready for extractor...")
      try {
        any23.extract(source, handler)
      } finally {
        handler.close
      }
      model
     }.fold(e => 
       CatsResource.liftF(err(s"Error obtaining RDF from HTML string: ${e.getMessage()}\nHTML String: ${htmlStr}\nExtractor name: ${extractorName}")), 
       model => {
         fromModel(model,None)
       } // Ref.of[IO,Model](model).flatMap(ref => )
     )
  } 

  private def fromModel(model: Model, uri: Option[IRI]): CatsResource[IO, RDFAsJenaModel] = {
    CatsResource.make(
      Ref.of[IO,Model](model).flatMap(ref => ok(RDFAsJenaModel(ref,None,None,Map(),Map())))
    )(
      m => m.getModel.flatMap(m => IO(m.close()))
    )
  }

  def extractFromUrl(uri: String, extractorName: String): CatsResource[IO,RDFReasoner] = {
    Try {
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent");
      val httpClient = any23.getHTTPClient;
      val source     = new HTTPDocumentSource(httpClient, uri)
      // val out = new ByteArrayOutputStream()
      val handler = JenaTripleHandler(model)
      try {
        any23.extract(source, handler)
      } finally {
        handler.close()
      }
      // val n3: String = out.toString("UTF-8")
      model
    }.fold(e =>
      CatsResource.liftF(err(s"Exception obtaining RDF from URI: ${e.getMessage}\nURI:\n$uri")),
      model => fromModel(model, Some(IRI(uri))) 
    ) 
  }

  case class JenaTripleHandler(m: Model) extends TripleHandler {

    override def receiveTriple(s: Resource,
                               p: RDF4jIRI,
                               o: Value,
                               g: RDF4jIRI,
                               context: ExtractionContext): Unit = {
      m.add(cnvSubj(s), cnvIRI(p), cnvObj(o))
      println(s"Triple: <${s},${p},${o}>")
    }
    override def startDocument(documentIRI: RDF4jIRI): Unit = {} // println(s"Start")
    override def openContext(context: ExtractionContext): Unit = {} // println(s"New context")
    override def receiveNamespace(prefix: String,uri: String,context: ExtractionContext): Unit = {}
    override def closeContext(context: ExtractionContext): Unit     = {}
    override def endDocument(documentIRI: RDF4jIRI): Unit = {}
    override def setContentLength(contentLength: Long): Unit = {}
    override def close(): Unit = {}

    def cnvSubj(r: Resource): JenaResource = r match {
      case i: RDF4jIRI => cnvIRI(i)
      case b: BNode => cnvBNode(b)
    }

    def cnvIRI(p:RDF4jIRI): JenaProperty =
      m.createProperty(p.toString)

    def cnvObj(o:Value): JenaRDFNode = o match {
      case i: RDF4jIRI => cnvIRI(i)
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
