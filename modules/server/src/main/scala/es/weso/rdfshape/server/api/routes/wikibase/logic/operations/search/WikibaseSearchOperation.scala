package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.WikibaseSearchOperation.{
  convertEntities,
  defaultSearchLanguage
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search.WikibaseSearchTypes.WikibaseSearchTypes
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

/** Common abstract class for wikibase operations based on searches: entities, properties, lexemes, etc.
  * Given an input [[WikibaseOperationDetails]], perform a search in a wikibase instance.
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note All derived operations are based on [[https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities]]
  */
private[wikibase] abstract class WikibaseSearchOperation(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO],
    itemType: WikibaseSearchTypes
) extends WikibaseOperation(
      WikibaseSearchOperation.successMessage,
      operationData,
      client
    ) {

  /** Target URL in the targeted wikibase instance. Already prepared with the
    * search action and given payload.
    */
  override lazy val targetUri: Uri = {
    targetWikibase.apiUrl
      .withQueryParam("action", "wbsearchentities")
      .withQueryParam("type", itemType)
      .withQueryParam("search", operationData.payload)
      .withQueryParam(
        "language",
        operationData.searchLanguage.getOrElse(defaultSearchLanguage)
      )
      .withQueryParam(
        "uselang",
        operationData.searchLanguage.getOrElse(defaultSearchLanguage)
      )
      .withQueryParam("limit", operationData.limit.getOrElse(defaultLimit))
      .withQueryParam(
        "continue",
        operationData.continue.getOrElse(defaultContinue)
      )
      .withQueryParam(
        "format",
        operationData.format
          .getOrElse(WikibaseOperationFormats.JSON)
      )
  }

  override def performOperation: IO[WikibaseOperationResult] = {
    // Build the results item from the wikibase response, throwing errors
    for {
      eitherResponse <- super.performRequest[Json]()
      result <- eitherResponse.flatMap(convertEntities) match {
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

private[wikibase] object WikibaseSearchOperation {
  private val successMessage = "Search executed successfully"

  /** Search language to be provided when none has been specified
    */
  private val defaultSearchLanguage = "en"

  /** Convert the response from wikibase's "wbsearchentities" to a
    * a JSON array for API responses
    *
    * @param json Input JSON, as received from Wikibase
    * @return Either a JSON representation of the entities in the Wikibase, or an error message
    */
  private[search] def convertEntities(json: Json): Either[String, Json] = for {
    entities <- json.hcursor
      .downField("search")
      .values
      .toRight("Error obtaining search value")
    converted = Json.fromValues(
      entities.map((value: Json) =>
        Json.fromFields(
          List(
            (
              "label",
              value.hcursor.downField("label").focus.getOrElse(Json.Null)
            ),
            ("id", value.hcursor.downField("id").focus.getOrElse(Json.Null)),
            (
              "uri",
              value.hcursor.downField("concepturi").focus.getOrElse(Json.Null)
            ),
            (
              "descr",
              value.hcursor.downField("description").focus.getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield converted

}
