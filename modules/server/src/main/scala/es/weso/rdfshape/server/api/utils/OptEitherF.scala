package es.weso.rdfshape.server.api.utils

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

/** Static utility methods to help work with Optional and Either types
  */
object OptEitherF {

  /** Given an input optional value (type A) and a conversor function (A => Either(String, B)),
    * attempt to convert the data and return an optional value (type B)
    *
    * @param maybe    Input data, optional value
    * @param function Conversion function from the input type to the output type
    * @tparam A Encapsulated type of the input data
    * @tparam B Encapsulated type of the output data
    * @return Optional value with the conversion result
    */
  def optEither2f[A, B](
      maybe: Option[A],
      function: A => Either[String, B]
  ): IO[Option[B]] = maybe match {
    case None => IO.pure(None)
    case Some(value) =>
      ApplicativeError[IO, Throwable].fromEither(
        // "FUNCTION" returns an either from the value in the option.
        function(value)
          .map(Some(_)) // If Either is a right, it is mapped to an Option type
          .leftMap(e =>
            new RuntimeException(s"Error: $e")
          ) // If Left, an exception in thrown
      )
  }

  /** Given an input optional value (type A) and a conversor function (A => Either(String, B)),
    * attempt to convert the data and return an either value (type B)
    *
    * @param maybe    Input data, optional value
    * @param function Conversion function from the input type to the output type
    * @tparam A Encapsulated type of the input data
    * @tparam B Encapsulated type of the output data
    * @return Either value with the conversion result/error
    */
  def optEither2es[A, B](
      maybe: Option[A],
      function: A => Either[String, B]
  ): EitherT[IO, String, Option[B]] = maybe match {
    case None        => EitherT.pure(None)
    case Some(value) => EitherT.fromEither(function(value).map(Some(_)))
  }

}
