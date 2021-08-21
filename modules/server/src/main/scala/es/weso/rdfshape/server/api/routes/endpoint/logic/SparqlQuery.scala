package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.PartsMap
import es.weso.rdfshape.server.api.routes.endpoint.logic.SparqlQueryTab.{
  SparqlQueryTab,
  defaultActiveQueryTab
}

import java.net.URL
import scala.io.Source
import scala.util.{Failure, Success, Try}

/** Data class representing a SPARQL query and its current source
  *
  * @param query          Query raw text
  * @param activeQueryTab Active tab, used to know which source the query comes from
  */
sealed case class SparqlQuery(
    query: String,
    activeQueryTab: SparqlQueryTab
)

private[api] object SparqlQuery extends LazyLogging {

  /** Placeholder value used for the sparql query whenever an empty query is issued/needed.
    */
  private val emptyQueryValue = ""

  /** Given a request's parameters, try to extract a SPARQL query from them
    *
    * @param partsMap Request's parameter
    * @return Either the SPARQL query or an error message
    */
  def getSparqlQuery(
      partsMap: PartsMap
  ): IO[Either[String, SparqlQuery]] =
    for {
      queryStr       <- partsMap.optPartValue("query")
      queryURL       <- partsMap.optPartValue("queryURL")
      queryFile      <- partsMap.optPartValue("queryFile")
      activeQueryTab <- partsMap.optPartValue("activeQueryTab")

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
          queryURL match {
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

  /** Error-safe way of obtaining the raw contents in a given URL
    *
    * @param urlString URL to be fetched (String representation)
    * @return Either the contents if the URL or an error message
    */
  private def getUrlContents(urlString: String): Either[String, String] = {
    Try {
      val url = new URL(urlString)
      val src = Source.fromURL(url)
      val str = src.mkString
      src.close()
      str
    } match {
      case Success(urlContent) => Right(urlContent)
      case Failure(exception) =>
        val msg =
          s"Error obtaining data from url $urlString: ${exception.getMessage}"
        logger.warn(msg)
        Left(msg)
    }
  }
}

/** Enumeration of the different possible QueryTabs sent by the client.
  * The tab sent indicates the API if the Query was sent in raw text, as a URL
  * to be fetched or as a text file containing the query.
  * In case the client submits the query in several formats, the selected tab will indicate the preferred format.
  */
private[logic] object SparqlQueryTab extends Enumeration {
  type SparqlQueryTab = String

  val TEXT = "#queryTextArea"
  val URL  = "#queryUrl"
  val FILE = "#queryFile"

  val defaultActiveQueryTab: SparqlQueryTab = TEXT
}
