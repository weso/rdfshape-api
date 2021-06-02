package es.weso.rdfshape.server.server

import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.triples.RDFTriple
import io.circe.Json

case class Outgoing(node: IRI, endpoint: IRI, children: Children) {
  def toJson: Json = Json.fromFields(
    List(
      ("node", Json.fromString(node.toString)),
      ("endpoint", Json.fromString(endpoint.toString)),
      (
        "children",
        Json.fromValues(
          children.m.map(pair =>
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

case class Children(m: Map[IRI, Vector[Value]])
case class Value(node: RDFNode, children: Children)

object Outgoing {
  val noChildren: Children = Children(Map())

  /** Creates an outgoing value from a set of triples.
    * It assumes all those triples have the same subject which is ignored
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

}
