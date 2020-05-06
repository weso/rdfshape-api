package es.weso.server.merged

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf._
import io.circe.Json
import cats.effect._
import fs2.Stream
import org.slf4j._
import cats.implicits._
// import fs2.Stream
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import _root_.es.weso.rdf.jena.RDFAsJenaModel
import _root_.es.weso.rdf.RDFReader
import _root_.es.weso.rdf.RDFReasoner
import _root_.es.weso.rdf.PrefixMap
import _root_.es.weso.rdf.nodes.IRI
import _root_.es.weso.rdf.nodes.RDFNode
import _root_.es.weso.rdf.path.SHACLPath
import _root_.es.weso.rdf.triples.RDFTriple
import _root_.es.weso.rdf.RDFBuilder


case class MergedModels(members: List[RDFAsJenaModel])
  extends RDFReader
     with RDFReasoner
     with LazyLogging {

  type Rdf = MergedModels

  lazy val mergedRdf: RDFAsJenaModel = {
      val zero: RDFAsJenaModel = RDFAsJenaModel(ModelFactory.createDefaultModel())
      def cmb(v: RDFAsJenaModel, r: RDFAsJenaModel): RDFAsJenaModel = RDFAsJenaModel(model = r.model.add(v.model))
      members.foldRight(zero)(cmb)
  }
exit
  lazy val prefixMap: PrefixMap = {
      val zero: PrefixMap = PrefixMap.empty
      def cmb(v: RDFAsJenaModel, pm: PrefixMap): PrefixMap = pm.addPrefixMap(v.getPrefixMap())
      members.foldRight(zero)(cmb)
  }

  val id = s"MergedModels"

  def availableParseFormats: List[String] = RDFAsJenaModel.availableFormats
  def availableSerializeFormats: List[String] = RDFAsJenaModel.availableFormats

  override def getPrefixMap: PrefixMap = {
    prefixMap
  }

  val log = LoggerFactory.getLogger("MergedModels")

  override def fromString(cs: CharSequence,
                          format: String,
                          base: Option[IRI]): RDFRead[MergedModels] = for {
    rdf <- RDFAsJenaModel.fromString(cs.toString, format, base)
  } yield MergedModels(List(rdf))

  override def serialize(format: String, base: Option[IRI]): RDFRead[String] = mergedRdf.serialize(format,base)

  override def iris(): RDFStream[IRI] =
    mergedRdf.iris()

  override def subjects(): RDFStream[RDFNode] =
    mergedRdf.subjects()

  override def predicates(): RDFStream[IRI] =
    mergedRdf.predicates()

  override def iriObjects(): RDFStream[IRI] =
    mergedRdf.iriObjects()

  override def getSHACLInstances(c: RDFNode): RDFStream[RDFNode] =
    mergedRdf.getSHACLInstances(c)

  override def hasSHACLClass(n: RDFNode, c: RDFNode): RDFRead[Boolean] = mergedRdf.hasSHACLClass(n,c)

  override def nodesWithPath(path: SHACLPath): RDFStream[(RDFNode, RDFNode)] =
    mergedRdf.nodesWithPath(path)


  override def subjectsWithPath(path: SHACLPath, obj: RDFNode): RDFStream[RDFNode] =
    mergedRdf.subjectsWithPath(path,obj)

  override def objectsWithPath(subj: RDFNode, path: SHACLPath): RDFStream[RDFNode] =
    mergedRdf.objectsWithPath(subj,path)

  override def checkDatatype(node: RDFNode, datatype: IRI): RDFRead[Boolean] =
    mergedRdf.checkDatatype(node,datatype)

  override def rdfTriples(): RDFStream[RDFTriple] =
    mergedRdf.rdfTriples()

  def triplesWithSubject(node: RDFNode): RDFStream[RDFTriple] =
    mergedRdf.triplesWithSubject(node)

  def triplesWithPredicate(p: IRI): RDFStream[RDFTriple] = 
    mergedRdf.triplesWithPredicate(p)

  def triplesWithObject(node: RDFNode): RDFStream[RDFTriple] = 
    mergedRdf.triplesWithObject(node)

  def triplesWithPredicateObject(p: IRI, o: RDFNode): RDFStream[RDFTriple] = 
    mergedRdf.triplesWithPredicateObject(p,o)

  // TODO: Not optimized...it just appends the inferred model to the end...  
  override def applyInference(inference: String): RDFRead[Rdf] = for {
    inferred <- mergedRdf.applyInference(inference)
  } yield MergedModels(List(inferred))

  override def availableInferenceEngines: List[String] = mergedRdf.availableInferenceEngines

  override def querySelect(queryStr: String): RDFStream[Map[String,RDFNode]] = 
     mergedRdf.querySelect(queryStr)

  override def queryAsJson(queryStr: String): RDFRead[Json] = 
    mergedRdf.queryAsJson(queryStr)

  override def getNumberOfStatements(): RDFRead[Int] = 
    mergedRdf.getNumberOfStatements()

  override def isIsomorphicWith(other: RDFReader): RDFRead[Boolean] =
    mergedRdf.isIsomorphicWith(other)

  override def asRDFBuilder: RDFRead[RDFBuilder] =
    mergedRdf.asRDFBuilder

  override def rdfReaderName: String = s"MergedModels"

  override def sourceIRI: Option[IRI] = None

  override def hasPredicateWithSubject(n: RDFNode, p: IRI): IO[Boolean] = mergedRdf.hasPredicateWithSubject(n,p)
  
}
