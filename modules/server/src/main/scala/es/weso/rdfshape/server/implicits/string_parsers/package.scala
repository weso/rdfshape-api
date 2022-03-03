package es.weso.rdfshape.server.implicits

import cats.Monad
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import es.weso.rdfshape.server.implicits.string_parsers.parsers.SchemaEngineParser
import es.weso.schema.{Schemas, Schema => SchemaW}
import org.http4s.rho.bits._

import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}

/** Custom string parsers used by Rho when parsing path parameters to complex data-types
  * @see https://github.com/http4s/rho/blob/main/core/src/main/scala/org/http4s/rho/bits/StringParser.scala
  */
package object string_parsers {

  case object parsers {

    /** Parser capable of extracting a [[SchemaW]] from a URL path
      * @tparam F Monad type used
      */
    class SchemaEngineParser[F[_]] extends StringParser[F, SchemaW] {
      override def parse(s: String)(implicit
          F: Monad[F]
      ): ResultResponse[F, SchemaW] = Try {
        Schemas.lookupSchema(s).unsafeRunSync()
      } match {
        case Failure(exception) =>
          FailureResponse.pure[F](BadRequest.pure(exception.getMessage))
        case Success(schema) => SuccessResponse(schema)
      }

      // Use String type tag
      override def typeTag: Option[universe.TypeTag[SchemaW]] = Some(
        implicitly[universe.TypeTag[SchemaW]]
      )
    }
  }

  case object instances {
    implicit val schemaEngineParser: SchemaEngineParser[IO] =
      new SchemaEngineParser[IO]
  }
}
