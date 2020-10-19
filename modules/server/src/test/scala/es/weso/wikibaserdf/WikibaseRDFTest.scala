package es.weso.wikibaserdfserver

import org.scalatest.funspec._
import org.scalatest.matchers.should._
import es.weso.rdf.nodes.IRI
import cats.implicits._
import cats.effect._
import cats.effect.concurrent._
import es.weso.rdf.nodes.RDFNode
import fs2.Stream
import es.weso.rdf.triples.RDFTriple
import org.apache.jena.rdf.model.{RDFNode => JenaRDFNode,Resource => JenaResource,_}

// import es.weso.server.WikibaseRDF
// import es.weso.rdf.jena.RDFAsJenaModel

class WikibaseRDFTest extends AnyFunSpec with Matchers {

/* class Model(name: String, var closed: Boolean) {
     def close(): Unit = this.closed = true

     def isClosed(): Boolean = this.closed
 }

 object ModelFactory {
     def createDefaultModel: Model = new Model("empty", false)
 } */


 case class RDFAsJenaModel(modelRef: Ref[IO,Model]) {
    
    def getModel: IO[Model] = modelRef.get

    def triplesWithSubject(node: RDFNode): Stream[IO,RDFTriple] = streamFromIOs(for {
        model <- getModel
        ts <- if (model.isClosed) {
         IO.raiseError(new RuntimeException(s"Closed model!!!"))
        } else 
         IO{
            pprint.log(node, s"Obtaining triples with Subject from ${node}")
            List[RDFTriple]()
        }
      } yield ts)

    def streamFromIOs[A](vs: IO[List[A]]): Stream[IO,A] = { 
     Stream.eval(vs).flatMap(x => Stream(x: _*))
 }  
 }

 def showJenaModel(rdf: RDFAsJenaModel): IO[Unit] = for {
     model <- rdf.getModel
 } yield {
     pprint.pprintln(s"RDF id: ${System.identityHashCode(rdf)}, Model id: ${System.identityHashCode(model)}")
 }

 object RDFAsJenaModel {
 
   def empty: IO[Resource[IO,RDFAsJenaModel]] = {
    def acquire: IO[RDFAsJenaModel] = {
      pprint.pprintln(s"### Acquire RDF")  
      val model = ModelFactory.createDefaultModel  
      pprint.log(System.identityHashCode(model),"Model Id in acquire")
      val jenaModel = RDFAsJenaModel.fromModel(model)
      pprint.log(System.identityHashCode(jenaModel),"RDF Id in acquire")
      jenaModel
    }
    IO(Resource.make(acquire)(closeJenaModel))
  }

  def fromModel(model: Model): IO[RDFAsJenaModel] = for {
    ref <- Ref.of[IO, Model](model)
  } yield { 
      val rdf = RDFAsJenaModel(ref)
      pprint.pprintln(s"FromModel ID: ${System.identityHashCode(rdf)})")
      rdf
  }

  private def closeJenaModel(m: RDFAsJenaModel): IO[Unit] = for {
    _ <- IO(pprint.log(m, s"Closing Model"))  
    model <- m.getModel
  } yield model.close()

 }

 case class CachedState(rdf: RDFAsJenaModel)

 object CachedState {
  def initial: IO[Resource[IO,CachedState]] = for {
      res <- RDFAsJenaModel.empty
  } yield res.evalMap(rdf => for {
      modelRef <- rdf.getModel
      _ <- showJenaModel(rdf)
      _ <- IO { pprint.log(modelRef,"CachedState.initial:")}
      _ <- IO { pprint.log(rdf,s"CachedState.initial: ${rdf} ${modelRef} Closed?:${modelRef.isClosed}")}
      _ <- showJenaModel(rdf)
    } yield CachedState(rdf))
 }


 case class WikibaseRDF(refCached: Ref[IO,CachedState]) {
     def triplesWithSubject(node: RDFNode): Stream[IO,RDFTriple] = for {
         cachedState <- Stream.eval(refCached.get)
         _ <- Stream.eval(showJenaModel(cachedState.rdf))
         ts <- cachedState.rdf.triplesWithSubject(node)
     } yield ts
 }

 object WikibaseRDF {
  
  val wikidata : IO[Resource[IO,WikibaseRDF]] = for {
      res <- CachedState.initial
  } yield res.evalMap(initial => for {
      ref <- Ref[IO].of(initial)
      _ <- IO(pprint.log(ref, "WikibaseRDF.wikidata"))
    } yield WikibaseRDF(ref))
 }


 describe(s"Use wikibaseRdf") {
   val item = IRI("http://www.wikidata.org/entity/Q29377880")
/*   it(s"Should use wikibase rdf once with the same item cached") {
       val r: IO[(Int,Int)] = WikibaseRDF.wikidata.use(wd => for {
           ts1 <- wd.triplesWithSubject(item).compile.toList
           ts2 <- wd.triplesWithSubject(item).compile.toList
       } yield ((ts1.length, ts2.length)))
       r.attempt.unsafeRunSync.fold(
         s => s"Error: ${s.getMessage}",
         pair => {
          val (n1,n2) = pair
          n1 should be (n2)
         }
       )
   } */

   it(s"Should use wikibase rdf twice") {
       val r: IO[((Int,Int),(Int,Int))] = for { 
         res <- WikibaseRDF.wikidata   
         pair1 <- res.use(wd => for {
           _ <- { pprint.log(wd,s"WD asking for item"); ().pure[IO]}  
           ts1 <- wd.triplesWithSubject(item).compile.toList
           _ <- { pprint.log(wd, s"Asking for item, 2nd time (cached?)"); ().pure[IO]}  
           ts2 <- wd.triplesWithSubject(item).compile.toList
         } yield ((ts1.length, ts2.length)))
         _ <- IO(pprint.pprintln("======================="))
         res2 <- WikibaseRDF.wikidata
         pair2 <- res2.use(wd => for {
           _ <- { pprint.log(wd, s"WD 2nd time"); ().pure[IO]}  
           ts1 <- wd.triplesWithSubject(item).compile.toList
           _ <- { pprint.log(wd,s"WD 2nd time 2nd round (cached?)"); ().pure[IO]}  
           ts2 <- wd.triplesWithSubject(item).compile.toList
         } yield ((ts1.length, ts2.length))) 
       } yield (pair1,pair2)
       r.attempt.unsafeRunSync.fold(
         s => fail(s"Error: ${s.getMessage}"),
         ppair => {
          val (n1,n2) = ppair
          info(s"Pairs: $ppair")
          n1 should be (n2)
         }
       )
   }

/*  ignore(s"Should obtain 2 empty RDFAsJenaModel's...")  {

     val r: IO[Unit] = RDFAsJenaModel.empty.use { case rdf1 => for {
         model1 <- rdf1.getModel
         _ <- IO (pprint.log(s"RDF1: ${rdf1}. IsClosed?: ${model1.isClosed}"))
         _ <- RDFAsJenaModel.empty.use { rdf2 => for {
          model2 <- rdf2.getModel
          _ <- IO(pprint.log(s"RDF1 (inside for): ${rdf1}. IsClosed?: ${model1.isClosed}"))
          _ <- IO(pprint.log(s"RDF2 (inside for): ${rdf2}. IsClosed?: ${model2.isClosed}"))
         } yield () }
      } yield () }
      r.attempt.unsafeRunSync.fold(
          s => fail(s"Error: $s"), 
          _ => info(s"Finnished")
      ) 
  } */

 }
}