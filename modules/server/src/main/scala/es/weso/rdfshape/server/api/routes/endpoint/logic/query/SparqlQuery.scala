package es.weso.rdfshape.server.api.routes.endpoint.logic.query

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuerySource.{
  SparqlQuerySource,
  defaultQuerySource
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  QueryParameter,
  QuerySourceParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing a SPARQL query and its current source
  *
  * @param queryPre    Query contents, as received before being processed depending on the [[querySource]]
  * @param querySource Active source, used to know which source the query comes from
  */
sealed case class SparqlQuery private (
    private val queryPre: Option[String],
    querySource: SparqlQuerySource
) extends LazyLogging {

  /** Given the (user input) for the query and its source, fetch the Query contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Either an error building the query text or a String containing the final text of the SPARQL query
    */
  lazy val rawQuery: Either[String, String] =
    queryPre.map(_.trim) match {
      case None | Some("") => Left("Could not build the query from empty data")

      case Some(userQuery) =>
        querySource match {
          case SparqlQuerySource.TEXT | SparqlQuerySource.FILE =>
            Right(userQuery)
          case SparqlQuerySource.URL =>
            getUrlContents(userQuery)

          case other =>
            val msg = s"Unknown query source: $other"
            logger.warn(msg)
            Left(msg)
        }
    }
}

private[api] object SparqlQuery extends LazyLogging {

  implicit val encodeSparqlQuery: Encoder[SparqlQuery] =
    (query: SparqlQuery) =>
      Json.obj(
        ("query", query.rawQuery.toOption.asJson),
        ("source", query.querySource.asJson)
      )

  /** Placeholder value used for the sparql query whenever an empty query is issued/needed.
    */
  private val emptyQuery = SparqlQuery(
    queryPre = None,
    querySource = defaultQuerySource
  )

  /** Given a request's parameters, try to extract a SPARQL query from them
    *
    * @param partsMap Request's parameters
    * @return Either the SPARQL query or an error message
    */
  def mkSparqlQuery(
      partsMap: PartsMap
  ): IO[Either[String, SparqlQuery]] =
    for {
      paramQuery     <- partsMap.optPartValue(QueryParameter.name)
      activeQueryTab <- partsMap.optPartValue(QuerySourceParameter.name)

      _ = logger.debug(
        s"Getting SPARQL from params. Query tab: $activeQueryTab"
      )

      maybeQuery <- mkSparqlQuery(
        paramQuery,
        activeQueryTab
      )

    } yield maybeQuery

  /** Create a SparqlQuery instance, given its source and data
    *
    * @param queryStr          Optionally, the contents of the query not processed by their source
    * @param activeQuerySource Optionally, the indicator of the query source (raw, url or file)
    * @return
    */
  private def mkSparqlQuery(
      queryStr: Option[String],
      activeQuerySource: Option[SparqlQuerySource]
  ): IO[Either[String, SparqlQuery]] =
    for {
      query <- IO {
        emptyQuery.copy(
          queryPre = queryStr,
          querySource = activeQuerySource.getOrElse(defaultQuerySource)
        )
      }
    } yield query.rawQuery.fold(
      // If the query text built is blank, an error occurred
      err => Left(err),
      _ => Right(query)
    )

}
