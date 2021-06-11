package es.weso.rdfshape.server.api.results

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.ApiHelper
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.utils.json.JsonUtilsServer._
import io.circe.Json

case class DataInfoResult private (
    msg: String,
    data: Option[String],
    dataFormat: Option[DataFormat],
    predicates: Option[Set[IRI]],
    numberStatements: Option[Int],
    prefixMap: Option[PrefixMap]
) {
  def toJson: Json = {
    Json.fromFields(
      List(("msg", Json.fromString(msg))) ++
        maybeField(data, "data", Json.fromString) ++
        maybeField(
          dataFormat,
          "dataFormat",
          (df: DataFormat) => Json.fromString(df.name)
        ) ++
        maybeField(numberStatements, "numberStatements", Json.fromInt) ++
        maybeField(prefixMap, "prefixMap", ApiHelper.prefixMap2Json) ++
        maybeField(
          predicates,
          "predicates",
          (preds: Set[IRI]) => Json.fromValues(preds.map(iri2Json))
        )
    )
  }

  lazy val pm = prefixMap.getOrElse(PrefixMap.empty)

  private def iri2Json(iri: IRI): Json = {
    Json.fromString(pm.qualifyIRI(iri))
  }

}

object DataInfoResult {
  def fromMsg(msg: String): DataInfoResult =
    DataInfoResult(msg, None, None, None, None, None)
  def fromData(
      data: Option[String],
      dataFormat: Option[DataFormat],
      predicates: Set[IRI],
      numberStatements: Int,
      prefixMap: PrefixMap
  ): DataInfoResult =
    DataInfoResult(
      "Well formed RDF",
      data,
      dataFormat,
      Some(predicates),
      Some(numberStatements),
      Some(prefixMap)
    )
}
