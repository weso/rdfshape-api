package es.weso.server
import Defaults._
import cats.data.EitherT
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import es.weso.rdf.RDFReasoner
import es.weso.schema.{Schema, Schemas}
import scala.io.Source
import scala.util.Try
import es.weso.rdf.nodes._
import es.weso.utils.IOUtils._
import es.weso.wikibase._
import org.http4s._
import org.http4s.Uri
import org.http4s.Charset._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.dsl._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.dsl.io._
import org.http4s.implicits._

case class WikibaseSchemaParam(
    maybeSchemaParam: Option[SchemaParam],
    maybeEntitySchema: Option[String],
    schemaStr: Option[String],
    wikibase: Wikibase = Wikidata
) {

  def getSchema(
      maybeData: Option[RDFReasoner],
      client: Client[IO]
  ): IO[(Option[String], Either[String, Schema])] = {
    (maybeSchemaParam, maybeEntitySchema) match {
      case (None, None) =>
        IO.pure((None, Left(s"No values for entity schema or schema")))
      case (Some(schemaParam), None) => schemaParam.getSchema(maybeData)
      case (None, Some(entitySchema)) =>
        schemaFromEntitySchema(entitySchema, client)
      case (Some(schemaParam), Some(entitySchema)) =>
        schemaFromEntitySchema(entitySchema, client)

    }
  }

  def schemaFromEntitySchema(
      es: String,
      client: Client[IO]
  ): IO[(Option[String], Either[String, Schema])] = {
    val uriSchema = wikibase.schemaEntityUri(es)
    val r: IO[(Schema, String)] = for {
      strSchema <- deref(uriSchema, client)
      schema    <- Schemas.fromString(strSchema, "ShEXC", "ShEx")
    } yield (schema, strSchema)
    r.attempt.map(_ match {
      case Left(t) => (None, Left(t.getMessage))
      case Right(pair) => {
        val (schema, str) = pair
        (Some(str), Right(schema))
      }
    })

  }

  private def deref(uri: Uri, client: Client[IO]): IO[String] = {
    val reqSchema: Request[IO] = Request(method = GET, uri = uri)
    client.expect[String](reqSchema)
  }
}

object WikibaseSchemaParam {

  private[server] def mkSchema(
      partsMap: PartsMap,
      data: Option[RDFReasoner],
      client: Client[IO]
  ): IO[(Schema, WikibaseSchemaParam)] = {
    val r: IO[(Schema, WikibaseSchemaParam)] = for {
      sp <- mkWikibaseSchemaParam(partsMap)
      p  <- sp.getSchema(data, client)
      (maybeStr, maybeSchema) = p
      res <- maybeSchema match {
        case Left(str) =>
          IO.raiseError(
            new RuntimeException(s"Error obtaining wikibase parameters: $str")
          )
        case Right(schema) => IO.pure((schema, sp.copy(schemaStr = maybeStr)))
      }
    } yield res
    r
  }

  private[server] def mkWikibaseSchemaParam(
      partsMap: PartsMap
  ): IO[WikibaseSchemaParam] =
    for {
      maybeSchema <- partsMap.eitherPartValue("entitySchema")
      // endpointStr      <- partsMap.partValue("endpoint")
      // endpoint         <- either2f(IRI.fromString(endpointStr))
      maybeSchemaParam <- SchemaParam.mkSchemaParam(partsMap).attempt
      result <- (maybeSchema, maybeSchemaParam) match {
        case (Left(_), Right(sp)) =>
          ok_f(WikibaseSchemaParam.empty.copy(maybeSchemaParam = Some(sp)))
        case (Right(s), Left(_)) =>
          ok_f(WikibaseSchemaParam.empty.copy(maybeEntitySchema = Some(s)))
        case (Right(s), Right(sp)) =>
          ok_f(
            WikibaseSchemaParam.empty
              .copy(maybeSchemaParam = Some(sp), maybeEntitySchema = Some(s))
          )
        case (Left(s), Left(errSp)) =>
          err_f(s"Error building schema param:\n${errSp}\n${s}")
      }
    } yield result

  private[server] def empty: WikibaseSchemaParam =
    WikibaseSchemaParam(None, None, None)

  // TODO: Move this code to es.weso.utils.IOUtils
  private def ok_f[A](v: A): IO[A] = IO.pure(v)
  private def err_f[A](err: String): IO[A] =
    IO.raiseError[A](new RuntimeException(err))
  private def either2f[A](e: Either[String, A]): IO[A] =
    e.fold(s => IO.raiseError(new RuntimeException(s)), IO.pure)

}
