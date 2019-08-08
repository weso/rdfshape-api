package es.weso.server

import cats.effect._
import es.weso._
import es.weso.schema._
import es.weso.server.QueryParams._
import org.http4s._
import org.http4s.multipart._
import org.http4s.twirl._
// import cats.effect.IO._
import cats.data.EitherT
import cats.implicits._
import es.weso.server.ApiHelper._
import es.weso.server.Defaults._
import es.weso.server.helper.{DataFormat, Svg}
import io.circe.Json
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.Router
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}
import org.http4s.server.staticcontent.{ResourceService, resourceService, webjarService}
import org.log4s.getLogger

class EndpointService[F[_]](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  private val relativeBase = Defaults.relativeBase

  private val logger = getLogger

  def routes(implicit timer: Timer[F]): HttpRoutes[F] = HttpRoutes.of[F] {

   case req@GET -> Root / "endpoint" / "query" :?
      OptQueryParam(optQuery) +&
        OptEndpointParam(optEndpoint)
    => {
      Ok("Not implemented yet")
   }

    case req@POST -> Root / "endpoint" / "query" => {
      req.decode[Multipart[F]] { m => {
      }
        Ok("Not implemented yet")
      }
    }

   case req@GET -> Root / "endpoint" / "outgoing" :?
     OptQueryParam(optQuery) +&
       OptEndpointParam(optEndpoint)
   => {
     Ok("Not implemented yet get neighbours of a node")
   }

   case req@POST -> Root / "endpoint" / "outgoing" => {
     req.decode[Multipart[F]] { m => {
     }
       Ok("Not implemented yet")
     }
   }

   case req@GET -> Root / "endpoint" / "validate" :?
     OptQueryParam(optQuery) +&
       OptEndpointParam(optEndpoint)
   => {
     Ok("Not implemented yet - validate node")
   }

   case req@POST -> Root / "endpoint" / "validate" => {
     req.decode[Multipart[F]] { m => {
     }
       Ok("Not implemented yet")
     }
   }

  }
}

object EndpointService {
  def apply[F[_]: Effect: ContextShift](blocker: Blocker): EndpointService[F] =
    new EndpointService[F](blocker)
}

