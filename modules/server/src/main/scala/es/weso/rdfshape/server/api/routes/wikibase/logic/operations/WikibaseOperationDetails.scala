package es.weso.rdfshape.server.api.routes.wikibase.logic.operations

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationFormats.WikibaseQueryFormat
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.codec.CodecUtils.uriEncoder
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.Uri
import org.http4s.implicits.http4sLiteralsSyntax

/** Case class representing the data attached to the queries made to wikibase's API
  * or SPARQL endpoint. Optional parameters may not be required in some operations.
  *
  * @param endpoint        [[Uri]] to be used to access a resource in a wikibase instance
  * @param payload         Data accompanying the request to the wikibase
  * @param searchLanguage  Tell the wikibase the language used in a search operation.
  * @param resultLanguages Filter the languages returned in queries with internationalized results
  *                        (empty list returns all available languages).
  *                        Each language must be represented by its language code.
  * @param limit           Maximum amount of results queried in search operations
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
    format: Option[WikibaseQueryFormat]
)

object WikibaseOperationDetails extends LazyLogging {

  /** Dummy empty query to be used when needed
    */
  val emptyQuery: WikibaseOperationDetails = WikibaseOperationDetails(
    endpoint = uri"",
    payload = "",
    searchLanguage = None,
    resultLanguages = None,
    limit = None,
    continue = None,
    format = None
  )

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

  /** Given a GET request's parameters, try to extract an instance
    * of [[WikibaseOperationDetails]] from them
    *
    * @param params Request's parameters
    * @return Either the [[WikibaseOperationDetails]] instance or an error message
    */
  def apply(
      params: Map[String, String]
  ): IO[Either[String, WikibaseOperationDetails]] = {
    // 1. Check for the existence of basic parameters: endpoint
    val wikibaseEndpoint = params.get(EndpointParameter.name)
    val payload          = params.get(WikibasePayloadParameter.name).map(_.strip())
    val queryMainData    = (wikibaseEndpoint, payload.getOrElse(""))

    // 2. Fill in with the rest of data, optionally absent
    val language = params.get(LanguageParameter.name).map(_.strip())
    val languages = params
      .get(LanguagesParameter.name)
      .map(
        _.strip().split('|').toList
      ) // "|" is the separating char for wikibase
    val limit    = params.get(LimitParameter.name).map(_.toInt)
    val continue = params.get(ContinueParameter.name).map(_.toInt)
    val format   = params.get(WikibaseFormatParameter.name).map(_.strip())

    IO {
      queryMainData match {
        case (Some(endpoint), payload) =>
          val endpointUri = Uri.fromString(endpoint.strip())
          endpointUri match {
            case Left(parseErr) =>
              parseErr.details.asLeft[WikibaseOperationDetails]
            case Right(uri) =>
              WikibaseOperationDetails(
                endpoint = uri,
                payload = payload,
                searchLanguage = language,
                resultLanguages = languages,
                limit = limit,
                continue = continue,
                format = format
              ).asRight[String]
          }

        case (None, payload) =>
          logger.warn(missingEndpointMessage)
          WikibaseOperationDetails(
            endpoint = Wikidata.baseUrl,
            payload = payload,
            searchLanguage = language,
            resultLanguages = languages,
            limit = limit,
            continue = continue,
            format = format
          ).asRight[String]
        case _ =>
          logger.error(unprocessableParamsMessage)
          Left(unprocessableParamsMessage)
      }
    }
  }

  /** Given a POST request's parameters, try to extract an instance
    * of [[WikibaseOperationDetails]] from them
    *
    * @param params Request's parameters
    * @return Either the [[WikibaseOperationDetails]] instance or an error message
    */
  def apply(
      params: PartsMap
  ): IO[Either[String, WikibaseOperationDetails]] = for {
    // 1. Check for the existence of endpoint and payload
    wikibaseEndpoint <- params.optPartValue(EndpointParameter.name)
    payload          <- params.optPartValue(WikibasePayloadParameter.name)
    queryMainData = (wikibaseEndpoint, payload.getOrElse(""))

    // 2. Fill in with the rest of data, optionally absent
    language  <- params.optPartValue(LanguageParameter.name)
    languages <- params.optPartValue(LanguagesParameter.name)
    limit     <- params.optPartValue(LimitParameter.name)
    continue  <- params.optPartValue(ContinueParameter.name)
    format    <- params.optPartValue(WikibaseFormatParameter.name)

  } yield queryMainData match {
    case (Some(endpoint), payload) =>
      val endpointUri = Uri.fromString(endpoint.strip())
      endpointUri match {
        case Left(parseErr) =>
          parseErr.details.asLeft[WikibaseOperationDetails]
        case Right(uri) =>
          WikibaseOperationDetails(
            endpoint = uri,
            payload = payload.strip(),
            searchLanguage = language.map(_.strip()),
            resultLanguages = languages.map(
              _.split('|').toList
            ),
            limit = limit.map(_.toInt),
            continue = continue.map(_.toInt),
            format = format.map(_.strip())
          ).asRight[String]
      }

    case (None, payload) =>
      logger.warn(missingEndpointMessage)
      WikibaseOperationDetails(
        endpoint = Wikidata.baseUrl,
        payload = payload.strip(),
        searchLanguage = language.map(_.strip()),
        resultLanguages = languages.map(
          _.split('|').toList
        ),
        limit = limit.map(_.toInt),
        continue = continue.map(_.toInt),
        format = format.map(_.strip())
      ).asRight[String]
    case _ =>
      logger.error(unprocessableParamsMessage)
      Left(unprocessableParamsMessage)
  }
}
