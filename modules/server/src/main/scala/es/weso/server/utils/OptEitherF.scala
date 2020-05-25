package es.weso.server.utils
import cats._ 
import cats.data._ 
import cats.effect._
import cats.implicits._

object OptEitherF {

 def optEither2f[A,B,F[_]:Effect](maybe: Option[A], fn: A => Either[String,B]): F[Option[B]] = maybe match {
    case None => Monad[F].pure(None)
    case Some(v) => ApplicativeError[F,Throwable].fromEither(fn(v).map(Some(_)).leftMap(e => new RuntimeException(s"Error: $e")))
 }

 def optEither2es[A,B,F[_]:Effect](maybe: Option[A], fn: A => Either[String,B]): EitherT[IO,String,Option[B]] = maybe match {
    case None => EitherT.pure(None)
    case Some(v) => EitherT.fromEither(fn(v).map(Some(_))) 
 }

}