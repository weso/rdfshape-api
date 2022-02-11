package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataQuery.{
  DataQueryResult,
  successMessage
}
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a data-information operation
  *
  * @param inputData  RDF input data (contains content and format information)
  * @param inputQuery Sparql query input
  * @param result     Object of type [[DataQueryResult]] containing the properties extracted from the data
  */

final case class DataQuery private (
    override val inputData: Data,
    inputQuery: SparqlQuery,
    result: DataQueryResult
) extends DataOperation(successMessage, inputData) {}

/** Static utilities to perform SPARQL queries on RDF data
  */
private[api] object DataQuery {
  private val successMessage = "Query executed successfully"

  /** Given an input data and query, perform the query on the data
    *
    * @param data  Input data to be queried
    * @param query Input SPARQL query
    * @return A [[DataQuery]] object with the query results (see also [[DataQueryResult]])
    */

  def dataQuery(data: Data, query: SparqlQuery): IO[DataQuery] =
    query.rawQuery match {
      case Left(err) => IO.raiseError(new RuntimeException(err))
      case Right(raw) =>
        for {
          rdf <- data.toRdf() // Get the RDF reader
          resultJson <- rdf.use(
            _.queryAsJson(raw)
          )                // Perform query
        } yield DataQuery( // Form the results object
          inputData = data,
          inputQuery = query,
          result = DataQueryResult(
            json = resultJson
          )
        )
    }

  /** Case class representing the results to be returned when performing a data-query operation
    * @note Currently limited to JSON formatted results for convenience
    */
  final case class DataQueryResult(
      json: Json
  )

  /** Encoder for [[DataQuery]]
    */
  implicit val encodeDataQueryOperation: Encoder[DataQuery] =
    (dataQuery: DataQuery) =>
      Json.fromFields(
        List(
          ("message", Json.fromString(dataQuery.successMessage)),
          ("data", dataQuery.inputData.asJson),
          ("query", dataQuery.inputQuery.asJson),
          ("result", dataQuery.result.json)
        )
      )

}
