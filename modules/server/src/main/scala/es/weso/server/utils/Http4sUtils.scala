package es.weso.server.utils

import cats.effect._
import cats._
import cats.implicits._
import fs2.Stream
import org.http4s.{Method, Request, Response, Uri}
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, Logger}

object Http4sUtils {

  def withRedirect[F[_]:Concurrent](c: Client[F]): Client[F] = FollowRedirect(10, _ => true)(c)
  def withLogging[F[_]:Concurrent: Async](client: Client[F]): Client[F] = Logger(true,true, _ => false)(client)
  def mkClient[F[_]:Concurrent: Async](c: Client[F]): Client[F] =
    withRedirect(withLogging(c))

  def getBody[F[_]:Monad: Concurrent](uri: Uri, r: Response[F]): F[Either[String,Stream[F,String]]] =
    if (r.status.isSuccess) r.bodyText.asRight.pure[F]
    else s"Status error fetching $uri: ${r.status}".asLeft.pure[F]

  def resolveStream[F[_]:Monad: Concurrent](uri: Uri,
                                            client: Client[F]): F[Either[String,Stream[F,String]]] = {
    val req = Request[F](Method.GET, uri)
    client.toHttpApp(req).flatMap(resp => getBody(uri,resp))
  }

}