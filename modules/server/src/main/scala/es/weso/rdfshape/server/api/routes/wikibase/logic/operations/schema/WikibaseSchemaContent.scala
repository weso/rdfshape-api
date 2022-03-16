package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import io.circe.syntax.EncoderOps
import org.http4s.Uri
import org.http4s.client.Client

/** Given an input [[WikibaseOperationDetails]], search for schemas in a
  * wikibase instance
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  */
private[wikibase] case class WikibaseSchemaContent(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseOperation(
      WikibaseSchemaContent.successMessage,
      operationData,
      client
    ) {

  /** Target URL in the targeted wikibase instance
    */
  override lazy val targetUri: Uri = {
    targetWikibase.baseUrl /
      "wiki" /
      "Special:EntitySchemaText" /
      operationData.payload
  }

  override def performOperation: IO[WikibaseOperationResult] = {
    // Build the results item from the wikibase response, throwing errors
    for {
      eitherResponse <- super
        .performRequest[String]()
      result <- eitherResponse match {
        case Left(err) => IO.raiseError(new WikibaseServiceException(err))
        case Right(jsonResults) =>
          IO {
            WikibaseOperationResult(
              operationData = operationData,
              wikibase = targetWikibase,
              result = jsonResults.asJson
            )
          }
      }
    } yield result
  }

}

object WikibaseSchemaContent {
  private val successMessage = "Schema contents fetched successfully"
}
