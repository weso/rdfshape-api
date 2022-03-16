package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get.WikibaseGetOperation.defaultResultLanguages
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get.WikibasePropTypes.WikibasePropTypes
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationFormats,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import io.circe.Json
import org.http4s.Uri
import org.http4s.circe.jsonDecoder
import org.http4s.client.Client

/** Common class for wikibase operations based on retrieving entities.
  * Given an input [[WikibaseOperationDetails]], get items from a wikibase instance.
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @param props         Properties to be returned from the objects retrieved
  * @note All derived operations are based on [[https://www.wikidata.org/w/api.php?action=help&modules=wbgetentities]]
  */
private[wikibase] class WikibaseGetOperation(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO],
    props: List[WikibasePropTypes] = WikibasePropTypes.default
) extends WikibaseOperation(
      WikibaseGetOperation.successMessage,
      operationData,
      client
    ) {

  /** Target URL in the targeted wikibase instance. Already prepared with the
    * search action and given payload.
    */
  override lazy val targetUri: Uri = {
    targetWikibase.apiUrl
      .withQueryParam("action", "wbgetentities")
      .withQueryParam("props", props.mkString("|"))
      .withQueryParam("ids", operationData.payload)
      .withQueryParam(
        "languages",
        operationData.resultLanguages
          .getOrElse(defaultResultLanguages)
          .mkString("|")
      )
      .withQueryParam(
        "format",
        operationData.format
          .getOrElse(WikibaseOperationFormats.JSON)
      )

    /* "props" parameter with the data to be returned to be defined in
     * sub-classes */

  }

  override def performOperation: IO[WikibaseOperationResult] = {
    // Build the results item from the wikibase response, throwing errors
    for {
      eitherResponse <- super.performRequest[Json]()
      result <- eitherResponse match {
        case Left(err) => IO.raiseError(new WikibaseServiceException(err))
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

object WikibaseGetOperation {
  private val successMessage = "Get entities executed successfully"

  /** Languages in which the results will be returned when none has been specified
    */
  private val defaultResultLanguages: List[String] = List("en")
}
