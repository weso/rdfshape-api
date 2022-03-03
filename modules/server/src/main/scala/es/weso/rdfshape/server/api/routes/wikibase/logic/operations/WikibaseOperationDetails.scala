package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationFormats.WikibaseOperationFormats
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.implicits.codecs.{decodeUri, encodeUri}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.Uri

import scala.util.Try

/** Case class representing the data attached to the queries made to wikibase's API
  * or SPARQL endpoint. Optional parameters may not be required in some operations.
  *
  * @param endpoint        [[Uri]] pointing to the wikibase instance root or SPARQL query endpoint
  * @param payload         Data accompanying the request to the wikibase
  * @param searchLanguage  Language to be used in a wikibase search operation
  *                        as well as the language for the returned results
  * @param resultLanguages Filter the languages returned in GET queries with internationalized results
  *                        (empty list returns all available languages)
  *                        Each language must be represented by its language code
  * @param limit           Maximum amount of results queried in search operations
  *                        In SPARQL queries, the limit is embedded in the query text
  * @param continue        Offset where to continue a search operation
  * @param format          Format in which results are requested
  * @see [[https://www.mediawiki.org/wiki/Wikibase/API#API_documentation_and_Wikibase_modules]]
  */
case class WikibaseOperationDetails(
    endpoint: Uri,
    payload: String,
    searchLanguage: Option[String],
    resultLanguages: Option[List[String]],
    limit: Option[Int],
    continue: Option[Int],
    format: Option[WikibaseOperationFormats]
) {
  // Non empty content
  assume(
    !endpoint.renderString.isBlank,
    "Could not build the Wikibase for an empty target"
  )
  assume(
    !payload.isBlank,
    "Empty payload for the Wikibase request"
  )
}

object WikibaseOperationDetails extends LazyLogging {

  /** Message to be logged/used when no endpoint was supplied
    */
  val missingEndpointMessage =
    "Missing endpoint for the wikibase operation, defaulting to Wikidata"

  /** Message to be logged/used when an unexpected error occurs
    * processing the parameters
    */
  val unprocessableParamsMessage =
    "An unexpected error occurred while processing the request parameters"

  /** JSON encoder for [[WikibaseOperationDetails]]
    * To be used for HTTP responses to clients
    */
  implicit val encode: Encoder[WikibaseOperationDetails] =
    (opDetails: WikibaseOperationDetails) =>
      Json.fromFields(
        List(
          ("endpoint", opDetails.endpoint.asJson),
          ("payload", opDetails.payload.asJson),
          ("searchLanguage", opDetails.searchLanguage.asJson),
          ("resultLanguages", opDetails.resultLanguages.asJson),
          ("limit", opDetails.limit.asJson),
          ("continue", opDetails.continue.asJson),
          ("format", opDetails.format.asJson)
        )
      )

  /** Decoder used to extract [[WikibaseOperationDetails]] instances from JSON values
    */
  implicit val decode: Decoder[Either[String, WikibaseOperationDetails]] =
    (cursor: HCursor) => {
      val operationInfo = for {
        // Default to wikidata if empty
        endpoint <- cursor
          .downField(EndpointParameter.name)
          .as[Either[String, Uri]] match {
          // Decode error, parameter was missing. Default to Wikidata
          case Left(_) =>
            logger.warn(missingEndpointMessage)
            Right(Wikidata.baseUrl).asRight
          // Extracted value, leave as is
          case other => other
        }

        payload <- cursor
          .downField(PayloadParameter.name)
          .as[String]

        searchLanguage <-
          cursor
            .downField(LanguageParameter.name)
            .as[Option[String]]

        resultLanguages <-
          cursor
            .downField(LanguagesParameter.name)
            .as[Option[List[String]]]

        limit <- cursor
          .downField(LimitParameter.name)
          .as[Option[Int]]

        continue <- cursor
          .downField(ContinueParameter.name)
          .as[Option[Int]]

        format <- cursor
          .downField(FormatParameter.name)
          .as[Option[WikibaseOperationFormats]]

      } yield (
        endpoint,
        payload,
        searchLanguage,
        resultLanguages,
        limit,
        continue,
        format
      )

      operationInfo.map {
        case (
              maybeUri,
              payload,
              optSearchLang,
              optResultLanguages,
              optLimit,
              optContinue,
              optFormat
            ) =>
          maybeUri.flatMap(uri =>
            Try {
              WikibaseOperationDetails(
                uri,
                payload,
                optSearchLang,
                optResultLanguages,
                optLimit,
                optContinue,
                optFormat
              )
            }.toEither.leftMap(err =>
              s"Could not build the Wikibase operation from user data:\n ${err.getMessage}"
            )
          )
      }
    }
}
