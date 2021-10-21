package es.weso.rdfshape.server.api.routes.data.logic.types.merged

import _root_.es.weso.rdf._
import _root_.es.weso.rdf.jena.RDFAsJenaModel
import _root_.es.weso.rdf.nodes.{IRI, RDFNode}
import _root_.es.weso.rdf.path.SHACLPath
import _root_.es.weso.rdf.triples.RDFTriple
import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import fs2._
import org.apache.jena.rdf.model.{Model, ModelFactory}

/** Data class representing an RDF model compound of several smaller RDF models
  *
  * @param members     RDF models conforming the merged model
  * @param mergedModel Unified RDF model containing the rest
  */
case class MergedModels(
    members: NonEmptyList[RDFAsJenaModel],
    mergedModel: Ref[IO, RDFAsJenaModel]
) extends RDFReader
    with RDFReasoner
    with LazyLogging {

  type Rdf = MergedModels
  val id = s"MergedModels"

  override def getPrefixMap: IO[PrefixMap] = {
    val zero: PrefixMap = PrefixMap.empty

    def cmb(pm: PrefixMap, v: RDFAsJenaModel): IO[PrefixMap] = for {
      newPm <- v.getPrefixMap
    } yield pm.addPrefixMap(newPm)

    members.foldM(zero)(cmb)
  }

  /** Available parse formats, similar to those in Jena models
    */
  def availableParseFormats: List[String] = RDFAsJenaModel.availableFormats

  /** Available serialize formats, similar to those in Jena models
    */
  def availableSerializeFormats: List[String] = RDFAsJenaModel.availableFormats

  /* Override and replicate the functionalities inherited from RDF
   * Reader/Reasoner */
  override def serialize(format: String, base: Option[IRI]): RDFRead[String] =
    for {
      mergedRdf <- getModel
      str       <- mergedRdf.serialize(format, base)
    } yield str

  /* override def fromString(cs: CharSequence, format: String, base:
   * Option[IRI]): RDFRead[MergedModels] = for { rdf <-
   * RDFAsJenaModel.fromString(cs.toString, format, base) } yield
   * MergedModels(List(rdf)) */

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
    getModel.flatMap(_.hasSHACLClass(n, c))

  override def nodesWithPath(path: SHACLPath): RDFStream[(RDFNode, RDFNode)] =
    Stream.eval(getModel).flatMap(_.nodesWithPath(path))

  override def subjectsWithPath(
      path: SHACLPath,
      obj: RDFNode
  ): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.subjectsWithPath(path, obj))

  override def objectsWithPath(
      subj: RDFNode,
      path: SHACLPath
  ): RDFStream[RDFNode] =
    Stream.eval(getModel).flatMap(_.objectsWithPath(subj, path))

  override def checkDatatype(node: RDFNode, datatype: IRI): RDFRead[Boolean] =
    getModel.flatMap(_.checkDatatype(node, datatype))

  override def rdfTriples(): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.rdfTriples())

  def triplesWithSubject(node: RDFNode): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.triplesWithSubject(node))

  def triplesWithPredicate(p: IRI): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.triplesWithPredicate(p))

  def triplesWithObject(node: RDFNode): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.triplesWithObject(node))

  def triplesWithPredicateObject(p: IRI, o: RDFNode): RDFStream[RDFTriple] =
    Stream.eval(getModel).flatMap(_.triplesWithPredicateObject(p, o))

  // TODO: Not optimized...it just appends the inferred model to the end...
  override def applyInference(inference: InferenceEngine): RDFRead[Rdf] = for {
    mergedRdf <- getModel
    /* engine <- InferenceEngine.fromString(inference) match { case Left(err) =>
     * IO.raiseError(new RuntimeException(s"Error parsing ${inference} as
     * inference engine: ${err}")) case Right(engine) => IO.pure(engine) } */
    inferred <- mergedRdf.applyInference(inference)
    ref      <- Ref.of[IO, RDFAsJenaModel](inferred)
  } yield MergedModels(members ++ List(inferred), ref)

  /* TODO: It only takes into account the inference engines available for the
   * first model */
  override def availableInferenceEngines: List[InferenceEngine] =
    members.head.availableInferenceEngines

  override def querySelect(queryStr: String): RDFStream[Map[String, RDFNode]] =
    Stream.eval(getModel).flatMap(_.querySelect(queryStr))

  override def queryAsJson(queryStr: String): RDFRead[Json] =
    getModel.flatMap(_.queryAsJson(queryStr))

  override def getNumberOfStatements(): RDFRead[Int] =
    getModel.flatMap(_.getNumberOfStatements())

  override def isIsomorphicWith(other: RDFReader): RDFRead[Boolean] =
    getModel.flatMap(_.isIsomorphicWith(other))

  def getModel: IO[RDFAsJenaModel] = mergedModel.get

  override def asRDFBuilder: RDFRead[RDFBuilder] =
    getModel.flatMap(_.asRDFBuilder)

  override def rdfReaderName: String = s"MergedModels"

  override def sourceIRI: Option[IRI] = None

  override def hasPredicateWithSubject(n: RDFNode, p: IRI): RDFRead[Boolean] =
    getModel.flatMap(_.hasPredicateWithSubject(n, p))

}

/** Static utilities to work with several RDF models
  */
object MergedModels {

  /** Merge multiple RDF sources, entered as a list, into one
    *
    * @param models List containing the RDF models to be merged
    * @return A unified RDF model containing all the models in the list
    */
  def fromList(models: List[RDFAsJenaModel]): IO[RDFReasoner] =
    NonEmptyList.fromList(models) match {
      case Some(nel) =>
        for {
          rdfModel    <- mergeModels(models)
          refRdfModel <- Ref.of[IO, RDFAsJenaModel](rdfModel)
        } yield MergedModels(nel, refRdfModel)
      case None =>
        for {
          ref <- Ref.of[IO, Model](ModelFactory.createDefaultModel())
        } yield RDFAsJenaModel(ref, None, None, Map(), Map())
    }

  /** Merge multiple RDF sources into one
    *
    * @param models List of RDF models to be merged
    * @return A unified RDF model containing all the models passed as arguments
    */
  private def mergeModels(models: List[RDFAsJenaModel]): IO[RDFAsJenaModel] = {
    val zero: Model = ModelFactory.createDefaultModel()

    def cmb(v: Model, r: Model): Model = r.add(v)

    for {
      model <- models.map(_.getModel).sequence.map(_.foldLeft(zero)(cmb))
      r     <- Ref.of[IO, Model](model)
    } yield RDFAsJenaModel(r, None, None, Map(), Map())
  }

}
