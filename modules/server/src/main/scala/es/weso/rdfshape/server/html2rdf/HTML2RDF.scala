package es.weso.rdfshape.server.html2rdf

import cats.effect.{Resource => CatsResource, _}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.utils.IOUtils._
import org.apache.any23.Any23
import org.apache.any23.extractor._
import org.apache.any23.extractor.microdata.MicrodataExtractor
import org.apache.any23.extractor.rdfa.RDFa11Extractor
import org.apache.any23.source.{HTTPDocumentSource, StringDocumentSource}
import org.apache.any23.writer._
import org.apache.jena.rdf.model.{
  Property => JenaProperty,
  RDFNode => JenaRDFNode,
  Resource => JenaResource,
  RDFReader => _,
  _
}
import org.eclipse.rdf4j.model.{
  BNode,
  Literal,
  Resource,
  Value,
  IRI => RDF4jIRI
}

import scala.util.Try

/** Utilities for extracting RDF models from different sources
  */
object HTML2RDF extends LazyLogging {

  /** List of all available RDF data extractors
    */
  val availableExtractors =
    List(RDFA11, Microdata)

  /** List of the names of all available RDF data extractors
    */
  val availableExtractorNames: List[String] =
    availableExtractors.map(_.name)

  /** For a given HTML string, extract the inner RDF data model
    *
    * @param htmlStr       HTML string
    * @param extractorName Name of the extractor to be used
    * @return RDF Reasoner allowing operations on the extracted RDF data
    */
  def extractFromString(
      htmlStr: String,
      extractorName: String
  ): CatsResource[IO, RDFReasoner] = {
    extractFromSource(RdfSourceTypes.STRING, htmlStr, extractorName)
  }

  /** For a given URI, extract its content's inner RDF data model
    *
    * @param uri           URI containing the RDF data
    * @param extractorName Name of the extractor to be used
    * @return RDF Reasoner allowing operations on the extracted RDF data
    */
  def extractFromUrl(
      uri: String,
      extractorName: String
  ): CatsResource[IO, RDFReasoner] = {
    extractFromSource(RdfSourceTypes.URI, uri, extractorName)
  }

  /** @param sourceType   Origin of the RDF data, used to perform different extraction operations
    * @param rdfData       String with the RDF data or the location of it
    * @param extractorName Name of the extractor to be used
    * @return RDF Reasoner allowing operations on the extracted RDF data
    */
  private def extractFromSource(
      sourceType: RdfSourceTypes.Value,
      rdfData: String,
      extractorName: String
  ): CatsResource[IO, RDFReasoner] = {
    Try {
      logger.debug(
        s"Extracting RDF from ${sourceType.toString} with extractor $extractorName"
      )

      // Common code to all RDF extractions
      val model = ModelFactory.createDefaultModel()
      val any23 = new Any23(extractorName)
      any23.setHTTPUserAgent("test-user-agent")
      val handler = JenaTripleHandler(model)

      // Check the RDF source and get the data accordingly
      val source = sourceType match {
        case RdfSourceTypes.STRING =>
          new StringDocumentSource(rdfData, "http://example.org/")
        case RdfSourceTypes.URI =>
          val httpClient = any23.getHTTPClient
          new HTTPDocumentSource(httpClient, rdfData)
      }

      // Extract RDF from data
      try {
        any23.extract(source, handler)
      } finally {
        handler.close()
      }
      // Return RDF model
      model
    }.fold(
      // Error handling
      e => {
        val errorMsg =
          s"Error obtaining RDF from HTML string: ${e.getMessage}\nHTML String: $rdfData\nExtractor name: $extractorName"
        logger.error(errorMsg)
        CatsResource.eval(
          err(errorMsg)
        )
      },
      model => {
        fromModel(model)
      }
    )
  }

  /** Get an RDF model object from a general Jena model
    *
    * @param model Input RDF model
    * @return RDF model
    */
  private def fromModel(model: Model): CatsResource[IO, RDFAsJenaModel] = {
    CatsResource.make(
      Ref
        .of[IO, Model](model)
        .flatMap(ref => ok(RDFAsJenaModel(ref, None, None, Map(), Map())))
    )(m => m.getModel.flatMap(m => IO(m.close())))
  }

  /** Interface comprising all RDF data extractors
    */
  sealed trait Extractor {
    val name: String
  }

  /** RDF triple handler based on Apache Jena
    *
    * @param model Base model
    */
  case class JenaTripleHandler(model: Model) extends TripleHandler {

    override def receiveTriple(
        s: Resource,
        p: RDF4jIRI,
        o: Value,
        g: RDF4jIRI,
        context: ExtractionContext
    ): Unit = {
      model.add(cnvSubj(s), cnvIRI(p), cnvObj(o))

      logger.debug(s"Triple: <$s,$p,$o>")
    }

    def cnvSubj(r: Resource): JenaResource = r match {
      case i: RDF4jIRI => cnvIRI(i)
      case b: BNode    => cnvBNode(b)
    }

    def cnvObj(o: Value): JenaRDFNode = o match {
      case i: RDF4jIRI => cnvIRI(i)
      case b: BNode    => cnvBNode(b)
      case l: Literal =>
        if(l.getLanguage.isPresent) {
          model.createLiteral(l.getLabel, l.getLanguage.get)
        } else
          model.createTypedLiteral(l.getLabel, l.getDatatype.toString)
    }

    def cnvBNode(b: BNode): JenaResource =
      model.createResource(AnonId.create(b.getID))

    def cnvIRI(p: RDF4jIRI): JenaProperty =
      model.createProperty(p.toString)

    override def startDocument(
        documentIRI: RDF4jIRI
    ): Unit = {}

    override def openContext(
        context: ExtractionContext
    ): Unit = {}

    override def receiveNamespace(
        prefix: String,
        uri: String,
        context: ExtractionContext
    ): Unit = {}

    override def closeContext(context: ExtractionContext): Unit = {}

    override def endDocument(documentIRI: RDF4jIRI): Unit = {}

    override def setContentLength(contentLength: Long): Unit = {}

    override def close(): Unit = {}

  }

  /** RDFA11 extractor
    */
  case object RDFA11 extends Extractor {
    val extractor    = new RDFa11Extractor()
    val name: String = extractor.getDescription.getExtractorName
  }

  /** Microdata extractor
    */
  case object Microdata extends Extractor {
    val extractor    = new MicrodataExtractor
    val name: String = extractor.getDescription.getExtractorName
  }
}
