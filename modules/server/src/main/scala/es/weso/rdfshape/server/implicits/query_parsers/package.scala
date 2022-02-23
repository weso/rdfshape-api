package es.weso.rdfshape.server.implicits

import cats.effect.IO
import es.weso.rdfshape.server.utils.json.JsonUtils.errorResponseJson
import org.http4s.Status._
import org.http4s.rho.bits.QueryParser.Params
import org.http4s.rho.bits.{FailureResponse, QueryParser, SuccessResponse}

import java.net.{MalformedURLException, URL}
import scala.util.{Failure, Success, Try}

/** Custom query parsers used by Rho when parsing query parameters to complex data-types
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
                      s"Invalid URL provided for shortening: $value",
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
}
