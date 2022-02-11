package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.languages

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.languages.WikibaseLanguages.convertLanguages
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

/** Given an input [[WikibaseOperationDetails]], query a wikibase instance
  * for all the languages present in it
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note All derived operations are based on [[https://www.wikidata.org/w/api.php?action=help&modules=query%2Bwbcontentlanguages]]
  */
private[wikibase] case class WikibaseLanguages(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseOperation(
      WikibaseLanguages.successMessage,
      operationData,
      client
    ) {

  /** Target URL in the targeted wikibase instance
    */
  override lazy val targetUri: Uri = {
    targetWikibase.apiUrl
      .withQueryParam("action", "query")
      .withQueryParam("meta", "wbcontentlanguages")
      .withQueryParam("wbclcontext", "term")
      .withQueryParam("wbclprop", "code|autonym")
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
      result <- eitherResponse.flatMap(convertLanguages) match {
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

object WikibaseLanguages {
  private val successMessage = "Messages fetched successfully"

  /** Convert the response from Wikibase "wbcontentlanguages" to a more convenient JSON structure
    *
    * @param json Input JSON, as received from Wikibase
    * @return Either a JSON representation of the languages in the Wikibase, or an error message
    */
  private def convertLanguages(json: Json): Either[String, Json] = for {
    languagesObj <- json.hcursor
      .downField("query")
      .downField("wbcontentlanguages")
      .focus
      .toRight(s"Error obtaining query/wbcontentlanguages at ${json.spaces2}")
    keys <- languagesObj.hcursor.keys.toRight(
      s"Error obtaining values from languages: ${languagesObj.spaces2}"
    )
    converted = Json.fromValues(
      keys.map(key =>
        Json.fromFields(
          List(
            (
              "label",
              languagesObj.hcursor
                .downField(key)
                .downField("code")
                .focus
                .getOrElse(Json.Null)
            ),
            (
              "name",
              languagesObj.hcursor
                .downField(key)
                .downField("autonym")
                .focus
                .getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield {
    converted
  }
}
