package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.data.EitherT
import cats.effect.IO
import es.weso.rdf.jena.{Endpoint => EndpointJena}
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.triples.RDFTriple
import es.weso.utils.IOUtils.{ESIO, stream2es}
import io.circe.{Encoder, Json}

import java.net.URL

case class Outgoing(node: IRI, endpoint: IRI, children: Children)

case class Children(m: Map[IRI, Vector[Value]])

case class Value(node: RDFNode, children: Children)

object Outgoing {
  val noChildren: Children = Children(Map())

  def getOutgoing(
      endpoint: URL,
      node: String,
      optLimit: Option[Int] = Some(1)
  ): EitherT[IO, String, Outgoing] = {
    for {
      endpointIRI <- EitherT.fromEither[IO](
        IRI.fromString(endpoint.toString)
      )
      node <- EitherT.fromEither[IO](
        IRI.fromString(node)
      )
      limit = optLimit.getOrElse(1)
      o <- outgoing(endpointIRI, node, limit)
    } yield o
  }

  def outgoing(endpoint: IRI, node: IRI, limit: Int): ESIO[Outgoing] =
    for {
      triples <- stream2es(EndpointJena(endpoint).triplesWithSubject(node))
    } yield Outgoing.fromTriples(node, endpoint, triples.toSet)

  /** Creates an outgoing value from a set of triples.
    * It assumes all those triples have the same subject which is ignored
    *
    * @param ts Triple set
    * @return
    */
  def fromTriples(node: IRI, endpoint: IRI, ts: Set[RDFTriple]): Outgoing = {
    val zero: Map[IRI, Vector[Value]] = Map()

    def cmb(
        m: Map[IRI, Vector[Value]],
        current: RDFTriple
    ): Map[IRI, Vector[Value]] =
      m.get(current.pred)
        .fold(
          m.updated(current.pred, Vector(Value(current.obj, noChildren)))
        )((vs: Vector[Value]) =>
          m.updated(current.pred, vs :+ Value(current.obj, noChildren))
        )

    Outgoing(node, endpoint, Children(ts.foldLeft(zero)(cmb)))
  }

  implicit val encode: Encoder[Outgoing] = (outgoing: Outgoing) =>
    Json.fromFields(
      List(
        (
          "node",
          Json.fromString(outgoing.node.toString)
        ),
        ("endpoint", Json.fromString(outgoing.endpoint.toString)),
        (
          "children",
          Json.fromValues(
            outgoing.children.m.map(pair =>
              Json.fromFields(
                List(
                  ("pred", Json.fromString(pair._1.toString)),
                  (
                    "values",
                    Json.fromValues(
                      pair._2.toList.map(value =>
                        Json.fromString(value.node.toString)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

}
