package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.query

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.{Headers, MediaType, Request, Uri}

/** Common class for wikibase operations based on querying a SPARQL endpoint.
  * Given an input [[WikibaseOperationDetails]], perform a query against a
  * wikibase instance.
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note All derived operations are based on [[https://www.wikidata.org/w/api.php?action=help&modules=wbgetentities]]
  */
private[wikibase] case class WikibaseQueryOperation(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseOperation(
      WikibaseQueryOperation.successMessage,
      operationData,
      client
    ) {

  /** Target URL in the targeted wikibase instance. Already prepared with the
    * endpoint and query.
    */
  override lazy val targetUri: Uri = {
    targetWikibase.queryUrl
      .withQueryParam("query", operationData.payload)
  }

  /** Request for this operation.
    * Include the "Accept" header to get JSON responses
    */
  override def request: Request[IO] =
    super.request.withHeaders(Headers(Accept(MediaType.application.`json`)))

  override def performOperation: IO[WikibaseOperationResult] = {
    // Build the results item from the wikibase response, throwing errors
    for {
      eitherResponse <- super.performRequest[Json]()
      result <- eitherResponse match {
        case Left(err) => IO.raiseError(WikibaseServiceException(err))
        case Right(jsonResults) =>
          IO {
            WikibaseOperationResult(
              operationData = operationData,
              wikibase = targetWikibase,
              result = jsonResults
            )
          }
      }
    } yield result
  }

}

object WikibaseQueryOperation {
  private val successMessage = "Query executed successfully"
}
