//package es.weso.rdfshape.server.api.routes.wikibase.logic
//
//import cats.effect._
//import com.typesafe.scalalogging.LazyLogging
//import es.weso.rdf.RDFReasoner
//import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
/* import
 * es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.WdSchemaParameter */
//import es.weso.rdfshape.server.api.utils.parameters.PartsMap
//import es.weso.rdfshape.server.wikibase._
//import es.weso.schema.{Schemas, Schema => SchemaW}
//import org.http4s._
//import org.http4s.client._
//import org.http4s.dsl.io._
//
//case class WikibaseSchema(
//    maybeSchemaParam: Option[Schema],
//    maybeEntitySchema: Option[String],
//    schemaStr: Option[String],
//    wikibase: Wikibase = Wikidata
//) {
//
//  def getSchema(
//      maybeData: Option[RDFReasoner],
//      client: Client[IO]
//  ): IO[(Option[String], Either[String, SchemaW])] = {
//    (maybeSchemaParam, maybeEntitySchema) match {
//      case (None, None) =>
//        IO.pure((None, Left(s"No values for entity schema or schema")))
//      case (Some(schemaParam), None) => schemaParam.getSchema()
//      case (None, Some(entitySchema)) =>
//        schemaFromEntitySchema(entitySchema, client)
//      case (Some(schemaParam), Some(entitySchema)) =>
//        schemaFromEntitySchema(entitySchema, client)
//
//    }
//  }
//
//  def schemaFromEntitySchema(
//      es: String,
//      client: Client[IO]
//  ): IO[(Option[String], Either[String, SchemaW])] = {
//    val uriSchema = wikibase.schemaEntityUri(es)
//    val r: IO[(SchemaW, String)] = for {
//      strSchema <- deref(uriSchema, client)
//      schema    <- Schemas.fromString(strSchema, "ShEXC", "ShEx")
//    } yield (schema, strSchema)
//    r.attempt.map {
//      case Left(t) => (None, Left(t.getMessage))
//      case Right(pair) =>
//        val (schema, str) = pair
//        (Some(str), Right(schema))
//    }
//
//  }
//
//  private def deref(uri: Uri, client: Client[IO]): IO[String] = {
//    val reqSchema: Request[IO] = Request(method = GET, uri = uri)
//    client.expect[String](reqSchema)
//  }
//}
//
//object WikibaseSchema extends LazyLogging {
//
//  private[api] def mkSchema(
//      partsMap: PartsMap,
//      data: Option[RDFReasoner],
//      client: Client[IO]
//  ): IO[(SchemaW, WikibaseSchema)] = {
//    val r: IO[(SchemaW, WikibaseSchema)] = for {
//      sp <- mkWikibaseSchemaParam(partsMap)
//      p  <- sp.getSchema(data, client)
//      (maybeStr, maybeSchema) = p
//      res <- maybeSchema match {
//        case Left(str) =>
//          IO.raiseError(
//            new RuntimeException(s"Error obtaining wikibase parameters: $str")
//          )
//        case Right(schema) => IO.pure((schema, sp.copy(schemaStr = maybeStr)))
//      }
//    } yield res
//    r
//  }
//
//  /** Build a [[WikibaseSchema]] from request parameters
//    *
//    * @param partsMap Request parameters
//    * @return Either the [[WikibaseSchema]] or an error constructing it
//    */
//  private[api] def mkWikibaseSchemaParam(
//      partsMap: PartsMap
//  ): IO[Either[String, WikibaseSchema]] =
//    for {
//      // WD Schema param as sent by client
//      paramWdSchema <- partsMap.optPartValue(WdSchemaParameter.name)
//      // endpointStr      <- partsMap.partValue("endpoint")
//      // endpoint         <- either2f(IRI.fromString(endpointStr))
//      maybeSchema <- Schema.mkSchema(partsMap)
//      result <- (paramWdSchema, maybeSchema) match {
//        case (None, Left(err)) =>
//          val msg =
//            s"Could not user supplied param and missing wdschema param: $err"
//          logger.error(msg)
//          IO.pure(Left(msg))
//
//        case (None, Right(schema)) =>
//          IO.pure(
//            Right(
//              WikibaseSchema.empty.copy(maybeSchemaParam = Option(schema))
//            )
//          )
//        case (Some(wdSchema), Left(err)) =>
//          logger.error(s"Could not build user supplied schema: $err")
//          IO.pure(
//            Right(
//              WikibaseSchema.empty.copy(maybeEntitySchema = Option(wdSchema))
//            )
//          )
//        case (Some(wdSchema), Right(schema)) =>
//          IO.pure(
//            Right(
//              WikibaseSchema.empty
//                .copy(
//                  maybeSchemaParam = Option(schema),
//                  maybeEntitySchema = Option(wdSchema)
//                )
//            )
//          )
//      }
//    } yield result
//
//  private[api] def empty: WikibaseSchema =
//    WikibaseSchema(None, None, None)
//}
