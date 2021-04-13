package es.weso.server.utils
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

object OptEitherF {

  def optEither2f[A, B](
      maybe: Option[A],
      fn: A => Either[String, B]
  ): IO[Option[B]] = maybe match {
    case None => IO.pure(None)
    case Some(v) =>
      ApplicativeError[IO, Throwable].fromEither(
        fn(v).map(Some(_)).leftMap(e => new RuntimeException(s"Error: $e"))
      )
  }

  def optEither2es[A, B](
      maybe: Option[A],
      fn: A => Either[String, B]
  ): EitherT[IO, String, Option[B]] = maybe match {
    case None    => EitherT.pure(None)
    case Some(v) => EitherT.fromEither(fn(v).map(Some(_)))
  }

}
