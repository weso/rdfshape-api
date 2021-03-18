package es.weso.server.merged

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf._
import io.circe.Json
import cats._
import cats.syntax.list._
import cats.effect._
import fs2._
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
import cats.data.NonEmptyList


case class MergedModels( members: NonEmptyList[RDFAsJenaModel], 
                         mergedModel: Ref[IO,RDFAsJenaModel]
                        )
  extends RDFReader
     with RDFReasoner
     with LazyLogging {

  type Rdf = MergedModels

  def getModel: IO[RDFAsJenaModel] = mergedModel.get

/*  def mergeModels: IO[RDFAsJenaModel] = {
      val zero: Model = RDFAsJenaModel(ModelFactory.createDefaultModel())
      def cmb(v: Model, r: Model): Model = RDFAsJenaModel(model = r.model.add(v.model))
      members.map(_.getModel) foldRight(zero)(cmb)
  } */

  override def getPrefixMap: IO[PrefixMap] = {
      val zero: PrefixMap = PrefixMap.empty
      def cmb(pm: PrefixMap, v: RDFAsJenaModel): IO[PrefixMap] = for {
       newPm <- v.getPrefixMap
      } yield pm.addPrefixMap(newPm)
        
      members.foldM(zero)(cmb)
  }

  val id = s"MergedModels"

  def availableParseFormats: List[String] = RDFAsJenaModel.availableFormats
  def availableSerializeFormats: List[String] = RDFAsJenaModel.availableFormats

  val log = LoggerFactory.getLogger("MergedModels")

/*  override def fromString(cs: CharSequence,
                          format: String,
                          base: Option[IRI]): RDFRead[MergedModels] = for {
    rdf <- RDFAsJenaModel.fromString(cs.toString, format, base)
  } yield MergedModels(List(rdf)) */

  override def serialize(format: String, base: Option[IRI]): RDFRead[String] = for {
    mergedRdf <- getModel
    str <- mergedRdf.serialize(format,base)
 } yield str

  override def iris(): RDFStream[IRI] = {
    Stream.eval(getModel).flatMap(_.iris())
  }

  override def subjects(): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.subjects())

  override def predicates(): RDFStream[IRI] =
    Stream.eval(getModel).flatMap(_.predicates())

  override def iriObjects(): RDFStream[IRI] =
    Stream.eval(getModel).flatMap(_.iriObjects())

  override def getSHACLInstances(c: RDFNode): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.getSHACLInstances(c))

  override def hasSHACLClass(n: RDFNode, c: RDFNode): RDFRead[Boolean] = 
    getModel.flatMap(_.hasSHACLClass(n,c))

  override def nodesWithPath(path: SHACLPath): RDFStream[(RDFNode, RDFNode)] =
    Stream.eval(getModel).flatMap(_.nodesWithPath(path))


  override def subjectsWithPath(path: SHACLPath, obj: RDFNode): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.subjectsWithPath(path,obj))

  override def objectsWithPath(subj: RDFNode, path: SHACLPath): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.objectsWithPath(subj,path))

  override def checkDatatype(node: RDFNode, datatype: IRI): RDFRead[Boolean] =
    getModel.flatMap(_.checkDatatype(node,datatype))

  override def rdfTriples(): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.rdfTriples())

  def triplesWithSubject(node: RDFNode): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.triplesWithSubject(node))

  def triplesWithPredicate(p: IRI): RDFStream[RDFTriple] = 
    Stream.eval(getModel).flatMap(_.triplesWithPredicate(p))

  def triplesWithObject(node: RDFNode): RDFStream[RDFTriple] = 
    Stream.eval(getModel).flatMap(_.triplesWithObject(node))

  def triplesWithPredicateObject(p: IRI, o: RDFNode): RDFStream[RDFTriple] = 
    Stream.eval(getModel).flatMap(_.triplesWithPredicateObject(p,o))

  // TODO: Not optimized...it just appends the inferred model to the end...  
  override def applyInference(inference: InferenceEngine): RDFRead[Rdf] = for {
    mergedRdf <- getModel
/*    engine <- InferenceEngine.fromString(inference) match {
      case Left(err) => IO.raiseError(new RuntimeException(s"Error parsing ${inference} as inference engine: ${err}"))
      case Right(engine) => IO.pure(engine)
    } */
    inferred <- mergedRdf.applyInference(inference)
    ref <- Ref.of[IO,RDFAsJenaModel](inferred)
  } yield MergedModels(members ++ List(inferred),ref)

  // TODO: It only takes into account the inference engines available for the first model
  override def availableInferenceEngines: List[InferenceEngine] = 
    members.head.availableInferenceEngines

  override def querySelect(queryStr: String): RDFStream[Map[String,RDFNode]] = 
     Stream.eval(getModel).flatMap(_.querySelect(queryStr))

  override def queryAsJson(queryStr: String): RDFRead[Json] = 
    getModel.flatMap(_.queryAsJson(queryStr))

  override def getNumberOfStatements(): RDFRead[Int] = 
    getModel.flatMap(_.getNumberOfStatements())

  override def isIsomorphicWith(other: RDFReader): RDFRead[Boolean] =
    getModel.flatMap(_.isIsomorphicWith(other))

  override def asRDFBuilder: RDFRead[RDFBuilder] =
    getModel.flatMap(_.asRDFBuilder)

  override def rdfReaderName: String = s"MergedModels"

  override def sourceIRI: Option[IRI] = None

  override def hasPredicateWithSubject(n: RDFNode, p: IRI): RDFRead[Boolean] = 
    getModel.flatMap(_.hasPredicateWithSubject(n,p))
  
}

object MergedModels {
  def fromList(ls: List[RDFAsJenaModel]): IO[RDFReasoner] = NonEmptyList.fromList(ls) match {
    case Some(nel) => for {
     rdfModel <- mergeModels(ls)
     refRdfModel <- Ref.of[IO,RDFAsJenaModel](rdfModel)
    } yield MergedModels(nel,refRdfModel)
    case None => for { 
      ref <- Ref.of[IO,Model](ModelFactory.createDefaultModel())
    } yield RDFAsJenaModel(ref,None,None,Map(),Map())
  }

 private def mergeModels(ls: List[RDFAsJenaModel]): IO[RDFAsJenaModel] = {
      val zero: Model = ModelFactory.createDefaultModel()
      def cmb(v: Model, r: Model): Model = r.add(v)
   for { 
     model <- ls.map(_.getModel).sequence.map(_.foldLeft(zero)(cmb))
     r <- Ref.of[IO,Model](model)
   } yield RDFAsJenaModel(r,None,None,Map(),Map())
 }

}