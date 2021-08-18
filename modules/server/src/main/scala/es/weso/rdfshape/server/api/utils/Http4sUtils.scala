package es.weso.rdfshape.server.api.utils

import cats._
import cats.effect._
import cats.implicits._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, Logger}
import org.http4s.{Method, Request, Response, Uri}

/** Static utility methods to help work with http4s
  */
object Http4sUtils {

  /** Create a full-fledged http4s client from a base client object
    *
    * @param client Base http4s client
    * @tparam F Type of the data managed by the client
    * @return The client passed to the function with additional functionalities (follow redirects and logging)
    */
  def mkClient[F[_]: Concurrent: Async](client: Client[F]): Client[F] =
    withRedirect(withLogging(client))

  /** Create a redirecting http4s client from a base client object
    *
    * @param client       Base http4s client
    * @param maxRedirects Maximum number of redirects the client will follow
    * @tparam F Type of the data managed by the client
    * @return The client passed to the function with additional functionalities (follow redirects)
    */
  def withRedirect[F[_]: Concurrent](
      client: Client[F],
      maxRedirects: Int = 10
  ): Client[F] =
    FollowRedirect(maxRedirects, _ => true)(client)

  /** Create a logging http4s client from a base client object
    *
    * @param client Base http4s client
    * @tparam F Type of the data managed by the client
    * @return The client passed to the function with additional functionalities (logging data)
    */
  def withLogging[F[_]: Concurrent: Async](client: Client[F]): Client[F] =
    Logger(logHeaders = true, logBody = true, _ => false)(client)

  /** Given a URI and an http4s client, fetch the URI contents
    *
    * @param uri    URI with the resource to be resolved
    * @param client Http4s client object that will fetch the resource
    * @tparam F Type of the data managed by the client
    * @return Either the body of the resource in the URI as a data Stream (using FS2) or an error message
    */
  def resolveStream[F[_]: Monad: Concurrent](
      uri: Uri,
      client: Client[F]
  ): F[Either[String, Stream[F, String]]] = {
    val req = Request[F](Method.GET, uri)
    client.toHttpApp(req).flatMap(resp => getBody(uri, resp))
  }

  /** Given a client response, extract the response body from it
    *
    * @param uri      URI
    * @param response Response object
    * @tparam F Type of the data contained in the response
    * @return Either the client's response body as plain text or an error message
    */
  private def getBody[F[_]: Monad: Concurrent](
      uri: Uri,
      response: Response[F]
  ): F[Either[String, Stream[F, String]]] = {
    if(response.status.isSuccess) response.bodyText.asRight.pure[F]
    else s"Status error fetching $uri: ${response.status}".asLeft.pure[F]
  }

}
