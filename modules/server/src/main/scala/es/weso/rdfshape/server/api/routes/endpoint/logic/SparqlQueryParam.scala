package es.weso.rdfshape.server.api.routes.endpoint.logic

import cats.effect.IO
import es.weso.rdfshape.server.api.definitions.ApiDefaults.defaultActiveQueryTab
import es.weso.rdfshape.server.api.routes.PartsMap

import java.net.URL
import scala.io.Source
import scala.util.Try

/** Data class representing a SPARQL query
  *
  * @param query Query string
  */
case class SparqlQuery(query: String)

/** Data class representing the parameters needed for SPARQL querying
  *
  * @param queryRaw       Query raw text (optional)
  * @param queryURL       Query containing URL (optional)
  * @param queryFile      Query containing file (optional)
  * @param activeQueryTab Active tab, used to know which query source to use
  */
case class SparqlQueryParam(
    queryRaw: Option[String],
    queryURL: Option[String],
    queryFile: Option[String],
    activeQueryTab: Option[String]
) {

  def getSparqlQuery: (Option[String], Either[String, SparqlQuery]) = {
    activeQueryTab.getOrElse(defaultActiveQueryTab) match {
      case "#queryUrl" =>
        queryURL match {
          case None => (None, Left(s"No value for queryURL"))
          case Some(queryUrl) =>
            Try {
              val url = new URL(queryUrl)
              val src = Source.fromURL(url)
              val str = src.mkString
              src.close()
              str
            }.toEither match {
              case Left(err) =>
                (
                  None,
                  Left(
                    s"Error obtaining data from url $queryUrl: ${err.getMessage} "
                  )
                )
              case Right(str) => (Some(str), Right(SparqlQuery(str)))
            }
        }
      case "#queryFile" =>
        queryFile match {
          case None => (None, Left(s"No value for queryFile"))
          case Some(queryStr) =>
            (Some(queryStr), Right(SparqlQuery(queryStr)))
        }
      case "#queryTextArea" =>
        queryRaw match {
          case None => (None, Right(SparqlQuery("")))
          case Some(queryText) =>
            (Some(queryText), Right(SparqlQuery(queryText)))
        }
      case other => (None, Left(s"Unknown value for activeQueryTab: $other"))
    }
  }

}

object SparqlQueryParam {

  /** Given a request's parameters, try to extract a SPARQL query from them
    *
    * @param partsMap Request's parameter
    * @return The SPARQL query or an error message
    */
  private[api] def getSparqlQuery(
      partsMap: PartsMap
  ): IO[Either[String, (SparqlQuery, SparqlQueryParam)]] = for {
    qp <- mkQueryParam(partsMap)
  } yield {
    val (maybeStr, maybeQuery) = qp.getSparqlQuery
    maybeQuery match {
      case Left(str)    => Left(str)
      case Right(query) => Right((query, qp.copy(queryRaw = Some(query.query))))
    }
  }

  private[api] def mkQueryParam(partsMap: PartsMap): IO[SparqlQueryParam] =
    for {
      queryStr       <- partsMap.optPartValue("query")
      queryURL       <- partsMap.optPartValue("queryURL")
      queryFile      <- partsMap.optPartValue("queryFile")
      activeQueryTab <- partsMap.optPartValue("activeQueryTab")
    } yield SparqlQueryParam(
      queryStr,
      queryURL,
      queryFile,
      activeQueryTab
    )

}
