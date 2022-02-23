package es.weso.rdfshape.server.api.routes.endpoint.logic.query

import cats.effect.IO
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuerySource.{SparqlQuerySource, defaultQuerySource}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{ContentParameter, QueryParameter, QuerySourceParameter, SourceParameter}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import io.circe._
import io.circe.syntax.EncoderOps

import scala.util.Try

/** Data class representing a SPARQL query and its current source
  *
  * @param queryContent    Query contents, as received before being processed depending on the [[querySource]]
  * @param querySource Active source, used to know which source the query comes from
  */
sealed case class SparqlQuery private (
    private val queryContent: String,
    querySource: SparqlQuerySource
) extends LazyLogging {

  /** Given the (user input) for the query and its source, fetch the Query contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Either an error building the query text or a String containing the final text of the SPARQL query
    */
  lazy val fetchedQueryContents: Either[String, String] =
    querySource match {
      case SparqlQuerySource.URL =>
        getUrlContents(queryContent)

      // Text or file
      case _ =>
        Right(queryContent)

    }
    
  assume(fetchedQueryContents.isRight, 
    fetchedQueryContents.left.getOrElse("Unknown error"))

  /**
   * Raw query value, i.e.: the text forming the query
   * @note It is safely extracted fromm [[fetchedQueryContents]] after asserting 
   *       the contents are right
   */
  val rawQuery: String = fetchedQueryContents.toOption.get
  
  
}

private[api] object SparqlQuery extends LazyLogging {

  /**
   * Encoder [[SparqlQuery]] => [[Json]]
   */
  implicit val encoder: Encoder[SparqlQuery] =
    (query: SparqlQuery) =>
      Json.obj(
        ("query", query.fetchedQueryContents.toOption.asJson),
        ("source", query.querySource.asJson)
      )

  /**
   * Decoder [[Json]] => [[SparqlQuery]]
   * @note Returns an either whose left contains specific errors building the query
   */
  implicit val decoder: Decoder[Either[String, SparqlQuery]] =
    (cursor: HCursor) => {
      // Get request data
      val queryData = for {
        queryContent <- cursor
          .downField(ContentParameter.name)
          .as[String]
          .map(_.trim)
        querySource <- cursor
          .downField(SourceParameter.name)
          .as[SparqlQuerySource]

      } yield (queryContent, querySource)

      // Safety checks: non empty query content and valid query source
      queryData.map {
        // Destructure
        case (content, source) =>
          if(content.isBlank)
            "Could not build the query from empty data".asLeft[SparqlQuery]
          else if(
            source != SparqlQuerySource.TEXT && source != SparqlQuerySource.URL && source != SparqlQuerySource.FILE
          )
            s"Unknown query source: \"$source\"".asLeft[SparqlQuery]
          else {
            // Try to build a query, catch the exception as error message if needed
            Try {
              SparqlQuery(content, source)
            }.toEither.leftMap( err => s"Could not build the SPARQL query from user data:\n ${err.getMessage}")
          }
      }
    }

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
        SparqlQuery(
          queryContent = queryStr.getOrElse(""),
          querySource = activeQuerySource.getOrElse(defaultQuerySource)
        )
      }
    } yield query.fetchedQueryContents.fold(
      // If the query text built is blank, an error occurred
      err => Left(err),
      _ => Right(query)
    )

}
