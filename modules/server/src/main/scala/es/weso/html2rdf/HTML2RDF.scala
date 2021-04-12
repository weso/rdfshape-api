package es.weso.html2rdf
import cats.effect.{Resource => CatsResource, _}
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.utils.IOUtils._
import org.apache.any23.Any23
import org.apache.any23.extractor._
import org.apache.any23.extractor.microdata.MicrodataExtractor
import org.apache.any23.extractor.rdfa.RDFa11Extractor
import org.apache.any23.source.{HTTPDocumentSource, StringDocumentSource}
import org.apache.any23.writer._
// import cats.effect.concurrent._
import scala.util.Try
// import org.apache.jena.rdf.model._
import org.apache.jena.rdf.model.{
  Property => JenaProperty,
  RDFNode => JenaRDFNode,
  Resource => JenaResource,
  RDFReader => _,
  _
}
import org.eclipse.rdf4j.model.{BNode, Literal, Resource, Value, IRI => RDF4jIRI}

object HTML2RDF {

  val availableExtractors =
    List(RDFA11, Microdata)

  val availableExtractorNames: List[String] =
    availableExtractors.map(_.name)

  def extractFromString(htmlStr: String, extractorName: String): CatsResource[IO, RDFReasoner] = {
    Try {
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent");
      val httpClient = any23.getHTTPClient;
      val source     = new StringDocumentSource(htmlStr, "http://example.org/")
      val handler    = JenaTripleHandler(model)
      println("Initialization ready for extractor...")
      try {
        any23.extract(source, handler)
      } finally {
        handler.close()
      }
      model
    }.fold(
      e =>
        CatsResource.eval(
          err(
            s"Error obtaining RDF from HTML string: ${e.getMessage}\nHTML String: $htmlStr\nExtractor name: $extractorName"
          )
        ),
      model => {
        fromModel(model, None)
      } // Ref.of[IO,Model](model).flatMap(ref => )
    )
  }

  def extractFromUrl(uri: String, extractorName: String): CatsResource[IO, RDFReasoner] = {
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
    }.fold(
      e => CatsResource.eval(err(s"Exception obtaining RDF from URI: ${e.getMessage}\nURI:\n$uri")),
      model => fromModel(model, Some(IRI(uri)))
    )
  }

  private def fromModel(model: Model, uri: Option[IRI]): CatsResource[IO, RDFAsJenaModel] = {
    CatsResource.make(
      Ref.of[IO, Model](model).flatMap(ref => ok(RDFAsJenaModel(ref, None, None, Map(), Map())))
    )(m => m.getModel.flatMap(m => IO(m.close())))
  }

  sealed trait Extractor {
    val name: String
  }

  case class JenaTripleHandler(m: Model) extends TripleHandler {

    override def receiveTriple(s: Resource, p: RDF4jIRI, o: Value, g: RDF4jIRI, context: ExtractionContext): Unit = {
      m.add(cnvSubj(s), cnvIRI(p), cnvObj(o))
      println(s"Triple: <$s,$p,$o>")
    }

    def cnvSubj(r: Resource): JenaResource = r match {
      case i: RDF4jIRI => cnvIRI(i)
      case b: BNode    => cnvBNode(b)
    }

    def cnvObj(o: Value): JenaRDFNode = o match {
      case i: RDF4jIRI => cnvIRI(i)
      case b: BNode    => cnvBNode(b)
      case l: Literal =>
        if (l.getLanguage.isPresent) {
          m.createLiteral(l.getLabel, l.getLanguage.get)
        } else
          m.createTypedLiteral(l.getLabel, l.getDatatype.toString)
    }

    def cnvIRI(p: RDF4jIRI): JenaProperty =
      m.createProperty(p.toString)

    def cnvBNode(b: BNode): JenaResource =
      m.createResource(AnonId.create(b.getID))

    override def startDocument(documentIRI: RDF4jIRI): Unit = {} // println(s"Start")

    override def openContext(context: ExtractionContext): Unit = {} // println(s"New context")

    override def receiveNamespace(prefix: String, uri: String, context: ExtractionContext): Unit = {}

    override def closeContext(context: ExtractionContext): Unit = {}

    override def endDocument(documentIRI: RDF4jIRI): Unit = {}

    override def setContentLength(contentLength: Long): Unit = {}

    override def close(): Unit = {}

  }

  case object RDFA11 extends Extractor {
    val extractor    = new RDFa11Extractor()
    val name: String = extractor.getDescription.getExtractorName
  }

  case object Microdata extends Extractor {
    val extractor    = new MicrodataExtractor
    val name: String = extractor.getDescription.getExtractorName
  }
}
