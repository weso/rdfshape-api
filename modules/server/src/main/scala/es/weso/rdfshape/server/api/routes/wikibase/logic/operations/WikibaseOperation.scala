package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.{
  Wikibase,
  Wikidata
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperation.successMessage
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.query.WikibaseQueryOperation
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema.WikibaseSheXerExtract
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.WikibaseSearchOperation
import org.http4s.Method.GET
import org.http4s._
import org.http4s.client.Client

/** General definition of operations that operate on wikibase
  *
  * @param successMessage Message attached to the result of the operation
  * @param operationData  Data needed to perform the wikibase operation
  * @param client         [[Client]] object to be used in requests to wikibase
  */
private[operations] abstract class WikibaseOperation(
    val successMessage: String = successMessage,
    val operationData: WikibaseOperationDetails,
    val client: Client[IO]
) {

  /** Wikibase instance to be queried by this operation
    * Given the data needed for the wikibase operation, configure the target wikibase instance.
    * Pattern match along all possible operations and set the target's [[Wikibase.baseUrl]] and/or [[wikibase.Wikibase.queryUrl]]
    * as needed for each operation.
    *
    * For instance: a [[WikibaseSearchOperation]] operation will include the wikibase's base URL
    * whereas a WikibaseQuery operation will include the wikibase's SPARQL query URL
    *
    * @note Initial check for Wikidata URLs
    */
  lazy val targetWikibase: Wikibase =
    if(
      operationData.endpoint.equals(Wikidata.baseUrl) || operationData.endpoint
        .equals(Wikidata.queryUrl) || operationData.endpoint
        .equals(Wikidata.apiUrl)
    ) Wikidata
    else
      this match {
        /* In certain operations, assume the endpoint the client sent is the
         * SPARQL endpoint of the wikibase instance */
        case _: WikibaseQueryOperation | _: WikibaseSheXerExtract =>
          Wikibase(queryUrl = operationData.endpoint)
        /* We normally assume that the endpoint the client sent is the base URL
         * of the wikibase instance */
        case _ =>
          Wikibase(baseUrl = operationData.endpoint)
      }

  /** [[Uri]] containing the target URL to be queried by the operation.
    * This property is to be overridden to fit each operation's needs.
    */
  val targetUri: Uri

  /** Maximum amount of results queried in search operations
    */
  val defaultLimit: Int = 20

  /** Offset where to continue a search operation
    */
  val defaultContinue = 0

  /** Base request object that will be executed when performing the operation.
    * Meant to be overridden or complemented with additional configurations
    */
  def request: Request[IO] =
    Request(method = GET, uri = targetUri).withHeaders(Headers.empty)

  /** Makes the necessary requests against the [[targetWikibase]]
    * as configured in the [[targetUri]] and returns the obtained JSON results
    *
    * @param decoder [[EntityDecoder]] required for deserializing the response
    * @tparam R Desired type to which the response shall be decoded
    * @return Either the Json response from the [[targetWikibase]] or a textual error
    */

  def performRequest[R](request: Request[IO] = request)(implicit
      decoder: EntityDecoder[IO, R]
  ): IO[Either[String, R]] = {
    // Execute request and get results
    for {
      // Extract the request response
      eitherResp <- client.run(request).use {
        // Got a resource with a Response[IO]
        case Status.Successful(response) =>
          response.attemptAs[R].leftMap(_.message).value
        case failedResponse =>
          failedResponse
            .attemptAs[String]
            .fold(
              decodeErr => decodeErr.message,
              rawResponse =>
                s"Request $request failed with status ${failedResponse.status.code} " +
                  s"and body $rawResponse"
            )
            .map(_.asLeft[R])
      }
    } yield eitherResp
  }

  /** Executes the operation against [[targetWikibase]]
    *
    * @return A [[WikibaseOperationResult]] instance with the operation results
    */
  def performOperation: IO[WikibaseOperationResult]
}

/** Static utilities for [[WikibaseOperation]]
  */
private[operations] object WikibaseOperation {

  /** Error message for operations currently limited to Wikidata instead
    * of any wikibase instance
    */
  val wikidataOnlyMessage =
    "Cannot extract schemas from wikibase instances other than Wikidata"

  /** Dummy success message
    */
  private val successMessage = "Operation completed successfully"
}
