package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataInfo.{
  DataInfoResult,
  successMessage
}
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.utils.json.JsonUtils._
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a data-information operation
  *
  * @param inputData RDF input data (contains content and format information)
  * @param result    Object of type [[DataInfoResult]] containing the properties extracted from the data
  */

final case class DataInfo private (
    override val inputData: Data,
    result: DataInfoResult
) extends DataOperation(successMessage, inputData) {}

/** Static utilities to obtain information about RDF data
  */
private[api] object DataInfo {
  private val successMessage = "Well formed RDF"

  /** Given an input data, get information about it
    *
    * @param data Input Data object of any type (Simple, Compound...)
    * @return A [[DataInfo]] object with the information of the input data
    */

  def dataInfo(data: Data): IO[DataInfo] = for {
    rdf <- data.toRdf()
    info <- rdf.use(rdf =>
      for {
        nStatements <- rdf.getNumberOfStatements()
        predicates  <- rdf.predicates().compile.toList
        prefixMap   <- rdf.getPrefixMap
      } yield (nStatements, predicates, prefixMap)
    )

    (nStatements, predicates, prefixMap) = info

  } yield DataInfo(
    inputData = data,
    result = DataInfoResult(
      numberOfStatements = nStatements,
      prefixMap = prefixMap,
      predicates = predicates.toSet
    )
  )

  /** Case class representing the results to be returned when performing a data-info operation
    */
  final case class DataInfoResult private (
      numberOfStatements: Int,
      prefixMap: PrefixMap,
      predicates: Set[IRI]
  )

  /** Encoder for [[DataInfoResult]]
    */
  private implicit val encodeDataInfoResult: Encoder[DataInfoResult] =
    (dataInfo: DataInfoResult) =>
      Json.fromFields(
        List(
          ("numberOfStatements", dataInfo.numberOfStatements.asJson),
          ("prefixMap", prefixMap2Json(dataInfo.prefixMap)),
          (
            "predicates",
            Json.fromValues(
              dataInfo.predicates.map(iri2Json(_, Some(dataInfo.prefixMap)))
            )
          )
        )
      )

  /** Encoder for [[DataInfo]]
    */
  implicit val encodeDataInfoOperation: Encoder[DataInfo] =
    (dataInfo: DataInfo) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(dataInfo.successMessage)),
          ("data", dataInfo.inputData.asJson),
          (
            "result",
            dataInfo.result.asJson.deepMerge(
              Json.fromFields(
                List(("format", dataInfo.inputData.format.asJson))
              )
            )
          )
        )
      )

}
