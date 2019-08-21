package es.weso.server.results

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.server.ApiHelper
import es.weso.server.helper._
import es.weso.utils.json.JsonUtilsServer._
import io.circe.Json

case class DataInfoResult private ( msg: String,
                           data: Option[String],
                           dataFormat: Option[DataFormat],
                           predicates: Option[Set[IRI]],
                           numberStatements: Option[Int],
                           prefixMap: Option[PrefixMap]
                         ) {
  def toJson: Json = {
    Json.fromFields(List(("msg", Json.fromString(msg))) ++
      maybeField(data, "data", Json.fromString(_)) ++
      maybeField(dataFormat, "dataFormat", (df: DataFormat) => Json.fromString(df.name)) ++
      maybeField(numberStatements, "numberStatements", Json.fromInt(_)) ++
      maybeField(prefixMap, "prefixMap", ApiHelper.prefixMap2Json(_)) ++
      maybeField(predicates, "predicates", (preds: Set[IRI]) => Json.fromValues(preds.map(iri2Json(_))))
    )
  }

  lazy val pm = prefixMap.getOrElse(PrefixMap.empty)

  private def iri2Json(iri: IRI): Json = {
    Json.fromString(pm.qualifyIRI(iri))
  }

}

object DataInfoResult {
  def fromMsg(msg: String): DataInfoResult = DataInfoResult(msg,None,None,None,None,None)
  def fromData(data: Option[String],
            dataFormat: Option[DataFormat],
            predicates: Set[IRI],
            numberStatements: Int,
            prefixMap: PrefixMap): DataInfoResult =
    DataInfoResult("Well formed RDF",
      data,
      dataFormat,
      Some(predicates),
      Some(numberStatements),
      Some(prefixMap))
}
