package es.weso.server.utils
import cats.effect._
import cats._
import cats.data._
import cats.implicits._
import es.weso.utils.internal.CollectionCompat._
import fs2.Stream

object IOUtils {

    type ESIO[A] = EitherT[IO,String,A]

    def either2io[A](e: Either[String,A]): IO[A] = 
      e.fold(s => IO.raiseError(new RuntimeException(s)), IO(_)) 

    def ok_es[A](x:A): ESIO[A] = EitherT.pure(x)
    def fail_es[A](msg: String): ESIO[A] = EitherT.fromEither(msg.asLeft[A])
    def io2es[A](io:IO[A]): ESIO[A] = EitherT.liftF(io)
    def either2es[A](e:Either[String,A]): ESIO[A] = EitherT.fromEither(e)
    def stream2es[A](s: Stream[IO,A]): ESIO[LazyList[A]] = io2es(s.compile.to(LazyList))
    def print_es[A](msg:String): ESIO[Unit] = io2es(IO(println(msg)))
    def run_es[A](es: ESIO[A]): IO[Either[String,A]] = es.value

    type ESF[A,F[_]] = EitherT[F,String,A]

    def ok_esf[A, F[_]:Applicative](x:A): ESF[A,F] = EitherT.pure[F,String](x)
    def fail_ef[A, F[_]:Applicative](msg: String): ESF[A,F] = EitherT.fromEither[F](msg.asLeft[A])
    def f2es[A, F[_]:Applicative](fa: F[A]): ESF[A,F] = EitherT.liftF(fa)
    def io2esf[A,F[_]:Effect: LiftIO](io:IO[A]): ESF[A,F] = EitherT.liftF(LiftIO[F].liftIO(io))
    def either2ef[A,F[_]:Applicative](e:Either[String,A]): ESF[A,F] = EitherT.fromEither[F](e)
    def stream2ef[A,F[_]:Effect](s: Stream[F,A]): ESF[LazyList[A],F] = f2es(s.compile.to(LazyList))

    def run_esf[A,F[_]](es: ESF[A,F]): F[Either[String,A]] = es.value
    def run_esiof[A,F[_]:Effect](esio: ESIO[A]): F[Either[String,A]] = run_esf(esio2esf[A,F](esio))

    def esio2esf[A, F[_]: Effect](e: ESIO[A]): ESF[A,F] = {
      for {
        either <- io2esf[Either[String,A],F](e.value)
        r <- either2ef[A,F](either)  
      } yield r
    }

    def io2f[A,F[_]:LiftIO](io: IO[A]): F[A] = LiftIO[F].liftIO(io)

}
