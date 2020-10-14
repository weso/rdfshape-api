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

  def getSchema[F[_]: Effect](
      maybeData: Option[RDFReasoner],
      client: Client[F]
  ): F[(Option[String], Either[String, Schema])] = {
    (maybeSchemaParam, maybeEntitySchema) match {
      case (None, None)                            => Monad[F].pure((None, Left(s"No values for entity schema or schema")))
      case (Some(schemaParam), None)               => io2f(schemaParam.getSchema(maybeData))
      case (None, Some(entitySchema))              => schemaFromEntitySchema(entitySchema, client)
      case (Some(schemaParam), Some(entitySchema)) => schemaFromEntitySchema(entitySchema, client)

    }
  }

  def schemaFromEntitySchema[F[_]: Effect](
      es: String,
      client: Client[F]
  ): F[(Option[String], Either[String, Schema])] = {
    val uriSchema = wikibase.schemaEntityUri(es)
    val r: F[(Schema, String)] = for {
      strSchema <- deref(uriSchema, client)
      schema    <- io2f(Schemas.fromString(strSchema, "ShEXC", "ShEx"))
    } yield (schema, strSchema)
    r.attempt.map(_ match {
      case Left(t) => (None, Left(t.getMessage))
      case Right(pair) => {
        val (schema, str) = pair
        (Some(str), Right(schema))
      }
    })

  }

  private def deref[F[_]: Effect](uri: Uri, client: Client[F]): F[String] = {
    val reqSchema: Request[F] = Request(method = GET, uri = uri)
    client.expect[String](reqSchema)
  }
}

object WikibaseSchemaParam {

  private[server] def mkSchema[F[_]: Effect](
      partsMap: PartsMap[F],
      data: Option[RDFReasoner],
      client: Client[F]
  ): F[(Schema, WikibaseSchemaParam)] = {
    val r: F[(Schema, WikibaseSchemaParam)] = for {
      sp <- mkWikibaseSchemaParam(partsMap)
      p  <- sp.getSchema(data, client)
      (maybeStr, maybeSchema) = p
      res <- maybeSchema match {
        case Left(str)     => MonadError[F,Throwable].raiseError(new RuntimeException(s"Error obtaining wikibase parameters: $str"))
        case Right(schema) => Monad[F].pure((schema, sp.copy(schemaStr = maybeStr)))
      }
    } yield res
    r
  }

  private[server] def mkWikibaseSchemaParam[F[_]: Effect](partsMap: PartsMap[F]): F[WikibaseSchemaParam] =
    for {
      maybeSchema      <- partsMap.eitherPartValue("entitySchema")
      // endpointStr      <- partsMap.partValue("endpoint")
      // endpoint         <- either2f(IRI.fromString(endpointStr))
      maybeSchemaParam <- SchemaParam.mkSchemaParam(partsMap).attempt
      result <- (maybeSchema, maybeSchemaParam) match {
        case (Left(_), Right(sp)) => ok_f(WikibaseSchemaParam.empty.copy(maybeSchemaParam = Some(sp)))
        case (Right(s), Left(_))  => ok_f(WikibaseSchemaParam.empty.copy(maybeEntitySchema = Some(s)))
        case (Right(s), Right(sp)) =>
          ok_f(WikibaseSchemaParam.empty.copy(maybeSchemaParam = Some(sp), maybeEntitySchema = Some(s)))
        case (Left(s), Left(errSp)) => err_f(s"Error building schema param:\n${errSp}\n${s}")
      }
    } yield result

  private[server] def empty: WikibaseSchemaParam =
    WikibaseSchemaParam(None, None, None)

  // TODO: Move this code to es.weso.utils.IOUtils
  private def ok_f[F[_]: Effect, A](v: A): F[A] = Monad[F].pure(v)
  private def err_f[F[_]: Effect, A](err: String): F[A] =
    MonadError[F, Throwable].raiseError[A](new RuntimeException(err))
  private def either2f[F[_]: Effect, A](e: Either[String, A]): F[A] =
    e.fold(s => MonadError[F, Throwable].raiseError(new RuntimeException(s)), Monad[F].pure(_))

}
