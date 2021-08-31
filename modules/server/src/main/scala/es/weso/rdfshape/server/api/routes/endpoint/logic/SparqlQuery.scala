package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.endpoint.logic.SparqlQueryTab.{
  SparqlQueryTab,
  defaultActiveQueryTab
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ActiveQueryTabParameter,
  QueryFileParameter,
  QueryParameter,
  QueryURLParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents

/** Data class representing a SPARQL query and its current source
  *
  * @param query          Query raw text
  * @param activeQueryTab Active tab, used to know which source the query comes from
  */
sealed case class SparqlQuery private (
    query: String,
    activeQueryTab: SparqlQueryTab
)

private[api] object SparqlQuery extends LazyLogging {

  /** Placeholder value used for the sparql query whenever an empty query is issued/needed.
    */
  private val emptyQueryValue = ""

  /** Given a request's parameters, try to extract a SPARQL query from them
    *
    * @param partsMap Request's parameters
    * @return Either the SPARQL query or an error message
    */
  def getSparqlQuery(
      partsMap: PartsMap
  ): IO[Either[String, SparqlQuery]] =
    for {
      queryStr       <- partsMap.optPartValue(QueryParameter.name)
      queryUrl       <- partsMap.optPartValue(QueryURLParameter.name)
      queryFile      <- partsMap.optPartValue(QueryFileParameter.name)
      activeQueryTab <- partsMap.optPartValue(ActiveQueryTabParameter.name)

      _ = logger.debug(
        s"Getting SPARQL from params. Query tab: $activeQueryTab"
      )

      maybeQuery: Either[String, SparqlQuery] = activeQueryTab.getOrElse(
        defaultActiveQueryTab
      ) match {
        case SparqlQueryTab.TEXT =>
          queryStr match {
            case None => Left("No value for the query string")
            case Some(queryRaw) =>
              Right(SparqlQuery(queryRaw, SparqlQueryTab.TEXT))
          }
        case SparqlQueryTab.URL =>
          queryUrl match {
            case None => Left(s"No value for the query URL")
            case Some(queryUrl) =>
              getUrlContents(queryUrl) match {
                case Right(queryRaw) =>
                  Right(SparqlQuery(queryRaw, SparqlQueryTab.URL))
                case Left(err) => Left(err)
              }

          }
        case SparqlQueryTab.FILE =>
          queryFile match {
            case None => Left(s"No value for the query file")
            case Some(queryRaw) =>
              Right(SparqlQuery(queryRaw, SparqlQueryTab.FILE))
          }

        case other =>
          val msg = s"Unknown value for activeQueryTab: $other"
          logger.warn(msg)
          Left(msg)

      }

    } yield maybeQuery

}

/** Enumeration of the different possible QueryTabs sent by the client.
  * The tab sent indicates the API if the Query was sent in raw text, as a URL
  * to be fetched or as a text file containing the query.
  * In case the client submits the query in several formats, the selected tab will indicate the one format.
  */
private[logic] object SparqlQueryTab extends Enumeration {
  type SparqlQueryTab = String

  val TEXT = "#queryTextArea"
  val URL  = "#queryUrl"
  val FILE = "#queryFile"

  val defaultActiveQueryTab: SparqlQueryTab = TEXT
}
