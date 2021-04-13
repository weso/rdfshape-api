package es.weso.server
import cats.Applicative
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import es.weso.server.APIDefinitions._
import es.weso.server.QueryParams._
import es.weso.server.{Query => ServerQuery}
import es.weso.utils.IOUtils._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.multipart._
import org.log4s.getLogger

class EndpointService(client: Client[IO]) extends Http4sDsl[IO] {

  private val relativeBase = Defaults.relativeBase

  private val logger = getLogger

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req @ POST -> Root / `api` / "endpoint" / "query" => {
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
            println(
              s"Query to endpoint ${endpoint}: ${optQueryStr.getOrElse("")}"
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
    }

    case req @ POST -> Root / `api` / "endpoint" / "info" => {
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

    case req @ GET -> Root / "endpoint" / "outgoing" :?
        OptQueryParam(optQuery) +&
        OptEndpointParam(optEndpoint) => {
      Ok("Not implemented yet get neighbours of a node")
    }

    case req @ POST -> Root / "endpoint" / "outgoing" => {
      req.decode[Multipart[IO]] { m =>
        {}
        Ok("Not implemented yet")
      }
    }

    case req @ GET -> Root / "endpoint" / "validate" :?
        OptQueryParam(optQuery) +&
        OptEndpointParam(optEndpoint) => {
      Ok("Not implemented yet - validate node")
    }

    case req @ POST -> Root / "endpoint" / "validate" => {
      req.decode[Multipart[IO]] { m =>
        {}
        Ok("Not implemented yet")
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
