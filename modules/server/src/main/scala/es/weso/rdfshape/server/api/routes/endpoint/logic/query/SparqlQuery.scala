package es.weso.rdfshape.server.api.routes.endpoint.logic.query

import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuerySource.SparqlQuerySource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ContentParameter,
  SourceParameter
}
import es.weso.rdfshape.server.utils.networking.NetworkingUtils.getUrlContents
import io.circe._
import io.circe.syntax.EncoderOps

import scala.util.Try

/** Data class representing a SPARQL query and its current source
  *
  * @param content    Query contents, as received before being processed depending on the [[source]]
  * @param source Active source, used to know which source the query comes from
  */
sealed case class SparqlQuery private (
    private val content: String,
    source: SparqlQuerySource
) extends LazyLogging {

  // Non empty content
  assume(!content.isBlank, "Could not build the query from empty data")
  // Valid source
  assume(
    SparqlQuerySource.values.exists(_ equalsIgnoreCase source),
    s"Unknown query source: '$source'"
  )

  /** Given the (user input) for the query and its source, fetch the Query contents using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files, decode the input; for raw data, do nothing)
    *
    * @return Either an error building the query text or a String containing the final text of the SPARQL query
    */
  lazy val fetchedContents: Either[String, String] =
    if(source equalsIgnoreCase SparqlQuerySource.URL)
      getUrlContents(content)
    // Text or file
    else Right(content)

  assume(
    fetchedContents.isRight,
    fetchedContents.left.getOrElse("Unknown error")
  )

  /** Raw query value, i.e.: the text forming the query
    *
    * @note It is safely extracted fromm [[fetchedContents]] after asserting
    *       the contents are right
    */
  val raw: String = fetchedContents.toOption.get

}

private[api] object SparqlQuery extends LazyLogging {

  /** Encoder [[SparqlQuery]] => [[Json]]
    */
  implicit val encoder: Encoder[SparqlQuery] =
    (query: SparqlQuery) =>
      Json.obj(
        ("content", query.raw.asJson),
        ("source", query.source.asJson)
      )

  /** Decoder [[Json]] => [[SparqlQuery]]
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

      queryData.map {
        /* Destructure and try to build the object, catch the exception as error
         * message if needed */
        case (content, source) =>
          Try {
            SparqlQuery(content, source)
          }.toEither.leftMap(err =>
            s"Could not build the SPARQL query from user data:\n ${err.getMessage}"
          )
      }
    }
}
