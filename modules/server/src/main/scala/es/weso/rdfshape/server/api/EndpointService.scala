package es.weso.rdfshape.server.api
import cats.data.EitherT
import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.ApiDefinitions._
import es.weso.rdfshape.server.api.{Query => ServerQuery}
import es.weso.utils.IOUtils._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.multipart._

class EndpointService(client: Client[IO])
    extends Http4sDsl[IO]
    with LazyLogging {

  private val relativeBase = Defaults.relativeBase

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / `api` / "endpoint" / "query" =>
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)

        val r: EitherT[IO, String, Json] = for {
          ep <- EndpointParam.mkEndpoint(partsMap)
//          json = Json.Null
          endpoint <- ep.getEndpointAsRDFReader
          either <- EitherT
            .liftF[IO, String, Either[String, (ServerQuery, SparqlQueryParam)]](
              SparqlQueryParam.mkQuery(partsMap)
            )
          pair <- EitherT.fromEither[IO](either)
          (_, qp)     = pair
          optQueryStr = qp.query.map(_.str)
          json <- {
            logger.debug(
              s"Query to endpoint $endpoint: ${optQueryStr.getOrElse("")}"
            )
            io2es(endpoint.queryAsJson(optQueryStr.getOrElse("")))
          }
        } yield json

        for {
          either <- r.value
          resp <- either.fold(
            e => errJson(s"Error querying endpoint: ${e}"),
            json => Ok(json)
          )
        } yield resp
      }

    case req @ POST -> Root / `api` / "endpoint" / "info" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          val r: EitherT[IO, String, Json] = for {
            ep <- EndpointParam.mkEndpoint(partsMap)
            ei <- EitherT.liftF[IO, String, EndpointInfo](ep.getInfo(client))
          } yield ei.asJson
          for {
            either <- r.value
            resp <- either.fold(
              e => errJson(s"Error obtaining info on Endpoint ${e}"),
              json => Ok(json)
            )
          } yield resp
        }
      }

  }

  private def errJson(msg: String): IO[Response[IO]] =
    Ok(Json.fromFields(List(("error", Json.fromString(msg)))))

}

object EndpointService {
  def apply(client: Client[IO]): EndpointService =
    new EndpointService(client)
}
