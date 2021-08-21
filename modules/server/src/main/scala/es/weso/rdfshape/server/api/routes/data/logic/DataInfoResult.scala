package es.weso.rdfshape.server.api.routes.data.logic

import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.routes.data.logic.DataOperations.prefixMap2Json
import es.weso.rdfshape.server.utils.json.JsonUtils._
import io.circe.Json

/** Data class representing the output of an "information" operation
  *
  * @param msg              Output informational message after processing. Used in case of error.
  * @param data             RDF input data
  * @param dataFormat       RDF input data format
  * @param predicates       List of predicates of the RDF input
  * @param numberStatements Number of statements in the RDF input
  * @param prefixMap        Prefix map of the RDF input
  */
case class DataInfoResult private (
    msg: String,
    data: Option[String],
    dataFormat: Option[DataFormat],
    predicates: Option[Set[IRI]],
    numberStatements: Option[Int],
    prefixMap: Option[PrefixMap]
) {

  /** Prefix map: defaults to empty.
    */
  lazy val pm: PrefixMap = prefixMap.getOrElse(PrefixMap.empty)

  /** Convert an information result to its JSON representation
    * @return JSON information of the extraction result
    */
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
        maybeField(prefixMap, "prefixMap", prefixMap2Json) ++
        maybeField(
          predicates,
          "predicates",
          (preds: Set[IRI]) => Json.fromValues(preds.map(iri2Json))
        )
    )
  }

  /** @param iri IRI to be converted
    * @return JSON representation of the IRI
    */
  private def iri2Json(iri: IRI): Json = {
    Json.fromString(pm.qualifyIRI(iri))
  }

}

object DataInfoResult {

  /** Message attached to the result when created successfully
    */
  val successMessage = "Well formed RDF"

  /** @param msg Error message contained in the result
    * @return A DataInfoResult consisting of a single error message and no data
    */
  def fromMsg(msg: String): DataInfoResult =
    DataInfoResult(msg, None, None, None, None, None)

  /** @return A DataInfoResult, given all the parameters needed to build it (input, predicates, etc.)
    */
  def fromData(
      data: Option[String],
      dataFormat: Option[DataFormat],
      predicates: Set[IRI],
      numberStatements: Int,
      prefixMap: PrefixMap
  ): DataInfoResult =
    DataInfoResult(
      successMessage,
      data,
      dataFormat,
      Some(predicates),
      Some(numberStatements),
      Some(prefixMap)
    )
}
