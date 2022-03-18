package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikibase.WikibaseSchema
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikidata.WikidataSchema
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperationDetails,
  WikibaseOperationFormats,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import io.circe.syntax.EncoderOps
import io.circe.{Json, parser}
import org.http4s.Uri
import org.http4s.circe.jsonDecoder
import org.http4s.client.Client

/** A [[WikibaseSearchOperation]] searching for schemas in a wikibase instance.
  * Unlike other search operations, there is no straightforward method to get
  * schemas with their labels and descriptions, so this is our custom solution.
  * 1- Resorts to [[https://www.wikidata.org/w/api.php?action=help&modules=query%2Bsearch]]
  * to get the pageIds of the schemas that match the query (by name)
  * 2- Resorts to [[https://www.wikidata.org/w/api.php?action=help&modules=query]]
  * to query the pageIds of each schema and get more detailed information about them
  * 3- For each schema, adds the URL to get their ShEx content so that clients
  * can easily use them
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @see https://phabricator.wikimedia.org/T304070
  * @see https://github.com/weso/YASHE/blob/6fd2242e4fc524f7621584eecb19d821fafb3d85/doc/main.js#L104-L130
  */
sealed case class WikibaseSearchSchema private (
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseSearchOperation(
      operationData,
      client,
      WikibaseSearchTypes.SCHEMA
    ) {

  /** URL to be queried to get the page Ids of the matching schemas.
    * Already prepared and given payload.
    */
  override lazy val targetUri: Uri = {
    targetWikibase.apiUrl
      .withQueryParam("action", "query")
      .withQueryParam("list", "search")
      // It would be interesting to use parameters in the search (e.g.: intitle)
      // however the schema titles are not representative
      // https://stackoverflow.com/a/33281775/9744696
      .withQueryParam("srsearch", operationData.payload)
      .withQueryParam("srprop", "")       // Do not return unneeded data
      .withQueryParam("srnamespace", 640) // Return schemas
      .withQueryParam("srlimit", operationData.limit.getOrElse(defaultLimit))
      .withQueryParam(
        "sroffset",
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
      // Perform the default request against targetUri
      eitherSchemas <- super
        .performRequest[Json]()
        .flatMap {
          // Error in first request, raise
          case Left(err)           => IO.raiseError(new WikibaseServiceException(err))
          case Right(jsonResponse) =>
            // From the first request's response, get the schema pageIds.
            val pageIds = parsePageIds(jsonResponse)

            // Make the request to query for the schemas' details
            val request = super.request.withUri(mkSchemaDetailsUri(pageIds))
            super.performRequest[Json](request)
        }

      result <- eitherSchemas match {
        // Error in second request, raise
        case Left(err) => IO.raiseError(new WikibaseServiceException(err))
        // Create the final results item
        case Right(jsonResults) =>
          IO {
            WikibaseOperationResult(
              operationData = operationData,
              wikibase = targetWikibase,
              result = parseFinalResult(jsonResults)
            )
          }
      }
    } yield result
  }

  /** Given a JSON returned from a Wikibase query searching for schemas, extract
    * the page ids of the results
    *
    * @param input JSON to be parsed for pageIds, as returned from MediaWiki's API
    * @return List containing the page Ids of the resulting schemas
    * @note Example request to be used as input: https://www.wikidata.org/w/api.php?action=query&list=search&srsearch=human&srprop=&srnamespace=640&format=json&formatversion=2
    */
  private def parsePageIds(input: Json): List[Int] = input.hcursor
    .downField("query")
    .downField("search")
    .values
    .get
    .map(_.hcursor.get[Int]("pageid").getOrElse(-1))
    .filter(_ > 0) // If no pageId could be parsed for an item, filter it out
    .toList

  /** Transform the final JSON received from Wikibase into the JSON we want
    * to return to clients, including only meaningful information as needed
    *
    * @param input Input JSON, as returned from MediaWiki's API
    * @return New JSON object with the schemas ID, title, labels, etc.
    * @note Example request to be used as input: https://www.wikidata.org/w/api.php?action=query&pageids=63842892|66281815&prop=revisions&rvslots=*&rvprop=content&format=json
    */
  private def parseFinalResult(input: Json): Json = {
    // 1. First isolate the object with the results themselves from the metadata
    val resultsObject =
      input.hcursor.downField("query").get[Json]("pages").getOrElse(Json.Null)

    /* 2. In this object, each key is a schema pageId and contains another
     * object with all the schema info.
     * For each key, make the JSON object that fits our needs */
    val results = resultsObject.hcursor.keys
      .getOrElse(List.empty)
      .map(key => {
        val optResult =
          for {
            // Object with a single schema information
            schemaObject <- resultsObject.hcursor.get[Json](key)
            // Schema simplified name: EntitySchema:EXXX
            schemaName <- schemaObject.hcursor.get[String]("title")
            // URL used to access the schema,
            // e.g.: https://www.wikidata.org/wiki/EntitySchema:EXXX
            webUri = targetWikibase.baseUrl / "wiki" / schemaName
            // Memory model of the schema
            schemaDomainObject =
              if(targetWikibase == Wikidata) WikidataSchema(webUri)
              else new WikibaseSchema(targetWikibase, webUri)

            // Schema labels, parsed from String field
            schemaAdditionalInfo <- schemaObject.hcursor
              .downField("revisions")
              .downArray
              .downField("slots")
              .downField("main")
              .get[String]("*")
              .flatMap(parser.parse)

            schemaId <- schemaAdditionalInfo.hcursor.get[String]("id")
            // Labels object, each key being a language
            schemaLabels <- schemaAdditionalInfo.hcursor.get[Json]("labels")
            // Descriptions object, each key being a language
            schemaDescriptions <- schemaAdditionalInfo.hcursor.get[Json](
              "descriptions"
            )
            // Language requested by the client to return the correct label/desc
            clientLang = operationData.searchLanguage.getOrElse(defaultLanguage)

          } yield Json.fromFields(
            List(
              ("id", schemaId.asJson),
              (
                "label",
                // Try to get the label in the client language,
                // else try to fallback to english
                // else return nothing
                schemaLabels.hcursor
                  .get[Json](clientLang)
                  .fold(
                    _ =>
                      schemaLabels.hcursor
                        .get[Json](defaultLanguage)
                        .getOrElse(Json.Null),
                    identity
                  )
              ),
              (
                "descr",
                schemaDescriptions.hcursor
                  .get[Json](clientLang)
                  .fold(
                    _ =>
                      schemaLabels.hcursor
                        .get[Json](defaultLanguage)
                        .getOrElse(Json.Null),
                    identity
                  )
              ),
              (
                "webUri",
                schemaDomainObject.entityUri.renderString.asJson
              ),
              (
                "conceptUri",
                schemaDomainObject.contentUri.renderString.asJson
              )
            )
          )

        // Return null Json if object processing failed
        optResult.toOption.getOrElse(Json.Null)
      })

    // Return a JSON with the array of all items, filter out nulls
    Json.fromValues(results).deepDropNullValues
  }

  /** URL to be queried for the schema details (labels, descriptions)
    */
  private def mkSchemaDetailsUri(schemaPageIds: List[Int]): Uri =
    targetWikibase.apiUrl
      .withQueryParam("action", "query")
      .withQueryParam(
        "pageids",
        schemaPageIds.mkString(
          "|"
        ) // All schema page Ids separated by "|". Max is 50 items but we won't surpass it
      )
      .withQueryParam("prop", "revisions")
      .withQueryParam("rvslots", "*")
      .withQueryParam("rvprop", "content")
      .withQueryParam(
        "format",
        operationData.format
          .getOrElse(WikibaseOperationFormats.JSON)
      )
}
