package es.weso.rdfshape.server.implicits

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import es.weso.schema.{Schemas, Schema => SchemaW}
import org.http4s.Status._
import org.http4s.rho.bits.QueryParser.Params
import org.http4s.rho.bits._

import java.net.{MalformedURLException, URL}
import scala.util.{Failure, Success, Try}

/** Custom query parsers used by Rho when parsing query parameters to complex data-types
  * @see https://github.com/http4s/rho/blob/main/core/src/main/scala/org/http4s/rho/bits/QueryParser.scala
  */
package object query_parsers {

  /** Parse the given URL parameter to an instance of [[java.net.URL]]
    * or return an error response
    */
  implicit val urlQueryParser: QueryParser[IO, URL] =
    (name: String, params: Params, _: Option[URL]) =>
      params.get(name) match {
        case Some(Seq(value, _*)) if !value.isBlank =>
          Try {
            new URL(value)
          } match {
            case Success(urlObj) => SuccessResponse(urlObj)
            case Failure(exception) =>
              exception match {
                case _: MalformedURLException =>
                  FailureResponse.pure(
                    errorResponseJson(
                      s"Invalid URL provided: $value",
                      BadRequest
                    )
                  )
                case _ =>
                  FailureResponse.pure(
                    errorResponseJson(
                      s"Failed to parse the URL sent in the request: $value",
                      InternalServerError
                    )
                  )
              }
          }
        case _ =>
          FailureResponse.pure(
            errorResponseJson(s"Missing query param: $name", BadRequest)
          )
      }

  /** Parse the given URL parameter to an instance of [[SchemaW]]
    * or return an error response
    */
  implicit val schemaEngineQueryParser: QueryParser[IO, SchemaW] =
    (name: String, params: Params, _: Option[SchemaW]) =>
      params.get(name) match {
        case Some(Seq(value, _*)) if !value.isBlank =>
          Try {
            Schemas.lookupSchema(value).unsafeRunSync()
          } match {
            case Failure(exception) =>
              FailureResponse.pure(
                errorResponseJson(
                  exception.getMessage,
                  InternalServerError
                )
              )
            case Success(schema) => SuccessResponse(schema)
          }

        case _ =>
          FailureResponse.pure(
            errorResponseJson(s"Missing query param: $name", BadRequest)
          )
      }
}
