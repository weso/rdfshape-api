package es.weso.server

// import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import es.weso._
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.streams.Streams
import es.weso.server.QueryParams._
import es.weso.server.utils.Http4sUtils._
import es.weso.server.values._
import io.circe._
import io.circe.parser._
import fs2._
import org.http4s._
import org.http4s.Uri
import org.http4s.Charset._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.dsl._
import org.http4s.headers._
import org.http4s.multipart._
import org.http4s.twirl._
import org.http4s.implicits._
import es.weso.rdf.sgraph._
import APIDefinitions._
import es.weso.utils.IOUtils._
import org.http4s.client.middleware.FollowRedirect
import es.weso.shapemaps.{Status => _, _}
import es.weso.rdf.nodes.IRI
import es.weso.schemaInfer.SchemaInfer
import es.weso.schema.Schema
import es.weso.schemaInfer.InferOptions
import es.weso.shex.ResolvedSchema
import es.weso.shex.validator.Validator
import es.weso.schema.ShapeMapTrigger
import es.weso.utils.internal.CollectionCompat._
import scala.util.control.NoStackTrace
import scala.util.matching.Regex
import es.weso.wikibaserdf._
import ApiHelper._

class WikidataService(client: Client[IO]) extends Http4sDsl[IO] {

  val wikidataEntityUrl = uri"http://www.wikidata.org/entity"
  val apiUri            = uri"/api/wikidata/entity"
  val wikidataUri: Uri  = uri"https://query.wikidata.org/sparql"
  val defaultLimit      = 20
  val defaultContinue   = 0
  val redirectClient    = FollowRedirect(3)(client)

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / `api` / "wikidata" / "test" => {
      Ok("Wikidata Test")
    }

    case GET -> Root / `api` / "wikidata" / "entityLabel" :?
        WdEntityParam(entity) +&
        LanguageParam(language) =>
      val uri = Uri.unsafeFromString(
        s"https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels&ids=${entity}&languages=${language}&format=json"
      )
      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        either <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        resp <- Ok(either.fold(Json.fromString, identity))
      } yield resp

    case GET -> Root / `api` / "wikidata" / "schemaContent" :?
        WdSchemaParam(wdSchema) => {
      val uri = uri"https://www.wikidata.org".withPath(
        Uri.Path.unsafeFromString(s"/wiki/Special:EntitySchemaText/${wdSchema}")
      )

      println(s"wikidata/schemaContent: ${uri.toString}")
      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[String].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[String]
              )
        }
        json: Json = eitherValues.fold(
          e => Json.fromFields(List(("error", Json.fromString(e)))),
          s => Json.fromFields(List(("result", Json.fromString(s))))
        )
        resp <- Ok(json)
      } yield resp

    }

    case GET -> Root / `api` / "wikidata" / "searchEntity" :?
        OptEndpointParam(endpoint) +&
        LabelParam(label) +&
        LanguageParam(language) +&
        LimitParam(maybelimit) +&
        ContinueParam(maybeContinue) => {
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)

      val requestUrl = s"${endpoint.getOrElse("https://www.wikidata.org")}"
      println(requestUrl)
      val uri = Uri
        .fromString(requestUrl)
        .valueOr(throw _)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("format", "json")

      println(s"wikidata/searchEntity: ${uri.toString}")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- cnvEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp
    }

    case GET -> Root / `api` / "wikidata" / "searchProperty" :?
        OptEndpointParam(endpoint) +&
        LabelParam(label) +&
        LanguageParam(language) +&
        LimitParam(maybelimit) +&
        ContinueParam(maybeContinue) => {
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)

      val requestUrl = s"${endpoint.getOrElse("https://www.wikidata.org")}"
      val uri = Uri
        .fromString(requestUrl)
        .valueOr(throw _)
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("type", "property")
        .withQueryParam("format", "json")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- cnvEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp
    }

    case GET -> Root / `api` / "wikidata" / "searchLexeme" :?
        LabelParam(label) +&
        LanguageParam(language) +&
        LimitParam(maybelimit) +&
        ContinueParam(maybeContinue) => {
      val limit: String    = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)

      println(s"SearchLexeme!!")

      val uri = uri"https://www.wikidata.org"
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "wbsearchentities")
        .withQueryParam("search", label)
        .withQueryParam("language", language)
        .withQueryParam("limit", limit)
        .withQueryParam("continue", continue)
        .withQueryParam("type", "lexeme")
        .withQueryParam("format", "json")

//      println(s"wikidata/searchLexeme: ${uri.toString}")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- cnvEntities(json)
        } yield converted
        resp <- Ok(eitherResult.fold(Json.fromString, identity))
      } yield resp
    }

    case GET -> Root / `api` / "wikidata" / "languages" => {

      val uri = uri"https://www.wikidata.org"
        .withPath(Uri.Path.unsafeFromString("/w/api.php"))
        .withQueryParam("action", "query")
        .withQueryParam("meta", "wbcontentlanguages")
        .withQueryParam("wbclcontext", "term")
        .withQueryParam("wbclprop", "code|autonym")
        .withQueryParam("format", "json")

      println(s"wikidata/languages: ${uri.toString}")

      val req: Request[IO] = Request(method = GET, uri = uri)
      for {
        eitherValues <- client.run(req).use {
          case Status.Successful(r) =>
            r.attemptAs[Json].leftMap(_.message).value
          case r =>
            r.as[String]
              .map(b =>
                s"Request $req failed with status ${r.status.code} and body $b"
                  .asLeft[Json]
              )
        }
        eitherResult = for {
          json      <- eitherValues
          converted <- cnvLanguages(json)
        } yield converted
        resp <- Ok(
          eitherResult.fold(Json.fromString, identity)
        )
      } yield resp
    }

    case req @ POST -> Root / `api` / "wikidata" / "query" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          for {
            optQuery    <- partsMap.optPartValue("query")
            optEndpoint <- partsMap.optPartValue("endpoint")
            endpoint = optEndpoint.getOrElse(wikidataUri.toString())
            query    = optQuery.getOrElse("")
            req: Request[IO] =
              Request(
                method = GET,
                uri = Uri
                  .fromString(endpoint)
                  .valueOr(throw _)
                  .withQueryParam("query", query)
              )
                .withHeaders(
                  `Accept`(MediaType.application.`json`)
                )
            eitherValue <- client.run(req).use {
              case Status.Successful(r) =>
                r.attemptAs[Json].leftMap(_.message).value
              case r =>
                r.as[String]
                  .map(b =>
                    s"Request $req failed with status ${r.status.code} and body $b"
                      .asLeft[Json]
                  )
            }
            resp <- Ok(eitherValue.fold(Json.fromString, identity))
          } yield resp
        }
      }

    /* case GET -> Root / "testQ" => { val req: Request[F] = Request(uri =
     * wikidataEntityUrl / "Q33").withHeaders(Accept(MediaType.text.turtle))
     * client.toHttpApp(req).flatMap(resp => Ok(Streams.cnv(resp.body))) } */

    case GET -> Root / "testR" => {
      /* val req: Request[F] = Request(uri = wikidataEntityUrl /
       * "Q33").withHeaders(Accept(MediaType.text.turtle)) */
      Ok(Streams.getRaw(wikidataEntityUrl / "Q33"))
    }

    case GET -> Root / "testRM" => {
      /* val req: Request[F] = Request(uri = wikidataEntityUrl /
       * "Q33").withHeaders(Accept(MediaType.text.turtle)) */
      Ok(Streams.getRawWithModel(wikidataEntityUrl / "Q33"))
    }

    case GET -> Root / "wdEntity" :?
        OptEntityParam(optEntity) +&
        OptWithDotParam(optWithDot) => {
      val maybeDot = optWithDot.fold(false)(identity)
      for {
        result   <- wdEntity(optEntity, maybeDot)
        response <- Ok(html.wdEntity(result, WikidataEntityValue(optEntity)))
      } yield response
    }

    case req @ POST -> Root / "wdEntity" =>
      req.decode[Multipart[IO]] { m =>
        {
          val partsMap = PartsMap(m.parts)
          for {
            optEntity  <- partsMap.optPartValue("entity")
            optWithDot <- partsMap.optPartValue("withDot")
            result <- {
              val withDot = optWithDot.fold(false)(_.toLowerCase match {
                case "false" => false
                case "true"  => true
                case _       => false
              })
              wdEntity(optEntity, withDot)
            }
            response <- Ok(
              html.wdEntity(result, WikidataEntityValue(optEntity))
            )
          } yield response
        }
      }

    case req @ GET -> Root / "wdSchema" =>
      Ok("Not implemented wikidata schema yet")
    case req @ GET -> Root / "wdValidate" =>
      Ok("Not implemented wikidata validate yet")

    case req @ POST -> Root / `api` / "wikidata" / "extract" => {
      println(s"POST /api/wikidata/extract, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: EitherT[IO, String, Response[IO]] = for {
          label  <- EitherT(partsMap.eitherPartValue("entity"))
          info   <- either2es[InfoEntity](cnvEntity(label))
          _      <- { println(s"URI: ${info.uri}"); ok_esf[Unit, IO](()) }
          strRdf <- io2es(redirectClient.expect[String](info.uri))
          eitherInferred <- io2es(
            RDFAsJenaModel
              .fromString(strRdf, "TURTLE")
              .flatMap(
                _.use(rdf =>
                  for {
                    rdfSerialized <- rdf.serialize("TURTLE")
                    nodeSelector = RDFNodeSelector(IRI(label))
                    inferred <- SchemaInfer.runInferSchema(
                      rdf,
                      nodeSelector,
                      "ShEx",
                      IRI(s"http://example.org/Shape_${info.localName}"),
                      InferOptions.defaultOptions.copy(maxFollowOn = 3)
                    )
                  } yield inferred
                )
              )
          )
          pair <- either2es[(Schema, ResultShapeMap)](eitherInferred)
          shExCStr <- io2es({
            val (schema, _) = pair
            schema.serialize("SHEXC")
          })
          _    <- { println(s"ShExC str: ${shExCStr}"); ok_es[Unit](()) }
          resp <- io2es(Ok(mkExtractAnswer(shExCStr, label)))
        } yield resp
        for {
          either <- r.value
          resp   <- either.fold(s => Ok(errExtract(s)), r => IO.pure(r))
        } yield resp
      }
    }

    // This one doesn't work. It gives a timeout response
    case req @ POST -> Root / `api` / "wikidata" / "shexer" => {
      println(s"POST /api/wikidata/shexer, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: EitherT[IO, String, Response[IO]] = for {
          label      <- EitherT(partsMap.eitherPartValue("entity"))
          jsonParams <- either2es[Json](mkShexerParams(label))
          postRequest = Request[IO](
            method = POST,
            uri = uri"http://156.35.94.158:8081/shexer"
          ).withHeaders(`Content-Type`(MediaType.application.`json`))
            .withEntity[Json](jsonParams)
          _      <- { println(s"URI: ${jsonParams.spaces2}"); ok_es[Unit](()) }
          result <- f2es(redirectClient.expect[Json](postRequest))
          _      <- { println(s"Result\n${result.spaces2}"); ok_es[Unit](()) }
          resp   <- f2es(Ok(result))
        } yield resp
        for {
          either <- r.value
          resp   <- either.fold(s => Ok(errExtract(s)), r => IO.pure(r))
        } yield resp
      }
    }

    case req @ POST -> Root / `api` / "wikidata" / "validate" => {
      println(s"POST /api/wikidata/validate, Request: $req")
      req.decode[Multipart[IO]] { m =>
        val partsMap = PartsMap(m.parts)
        val r: IO[Response[IO]] = for {
          eitherItem <- partsMap.eitherPartValue("item")
          _          <- { pprint.log(eitherItem); IO.pure(()) }
          item       <- fromEither(eitherItem)
          _          <- { pprint.log(item); IO.pure(()) }
          info       <- fromEither(cnvEntity2(item))
          _          <- { pprint.log(info); IO.pure(()) }
          pair       <- WikibaseSchemaParam.mkSchema(partsMap, None, client)
          _          <- { pprint.log(pair); IO.pure(()) }
          (schema, wbp) = pair
          iriItem  <- fromEither(IRI.fromString(info.sourceUri))
          shapeMap <- fromEither(ShapeMap.empty.add(iriItem, Start))
          triggerMode = ShapeMapTrigger(shapeMap)
          result <- for {
            res1 <- WikibaseRDF.wikidata
            res2 <- RDFAsJenaModel.empty
            vv <- (res1, res2).tupled.use { case (rdf, builder) =>
              for {
                r    <- schema.validate(rdf, triggerMode, builder)
                json <- result2json(r)
              } yield json
            }
          } yield vv
          resp <- Ok(result)
        } yield resp
        r.attempt.flatMap(_.fold(s => Ok(errExtract(s.getMessage)), IO.pure(_)))
      }
    }
  }

  case class WikibaseServiceError(msg: String)
      extends RuntimeException(msg)
      with NoStackTrace

  private def fromEither[A](either: Either[String, A]): IO[A] = {
    either.fold(s => IO.raiseError(WikibaseServiceError(s)), IO.pure(_))
  }

  private def errExtract(msg: String): Json = {
    Json.fromFields(
      List(
        ("error", Json.fromString(msg))
      )
    )
  }

  private def mkExtractAnswer(result: String, entity: String): Json = {
    Json.fromFields(
      List(
        ("entity", Json.fromString(entity)),
        ("result", Json.fromString(result))
      )
    )
  }

  private def wikidataPrefixes: Either[String, Json] = {
    val json = """{
    "http://wikiba.se/ontology#": "wikibase",
    "http://www.bigdata.com/rdf#": "bd",
    "http://www.wikidata.org/entity/": "wd",
    "http://www.wikidata.org/prop/direct/": "wdt",
    "http://www.wikidata.org/prop/direct-normalized/": "wdtn",
    "http://www.wikidata.org/entity/statement/": "wds",
    "http://www.wikidata.org/prop/": "p",
    "http://www.wikidata.org/reference/": "wdref",
    "http://www.wikidata.org/value/": "wdv",
    "http://www.wikidata.org/prop/statement/": "ps",
    "http://www.wikidata.org/prop/statement/value/": "psv",
    "http://www.wikidata.org/prop/statement/value-normalized/": "psn",
    "http://www.wikidata.org/prop/qualifier/": "pq",
    "http://www.wikidata.org/prop/qualifier/value/": "pqv",
    "http://www.wikidata.org/prop/qualifier/value-normalized/": "pqn",
    "http://www.wikidata.org/prop/reference/": "pr",
    "http://www.wikidata.org/prop/reference/value/": "prv",
    "http://www.wikidata.org/prop/reference/value-normalized/": "prn",
    "http://www.wikidata.org/prop/novalue/": "wdno"
   }"""
    parse(json).leftMap(e => s"Error parsing prefixes: $e")
  }

  private def mkShexerParams(entity: String): Either[String, Json] = for {
    prefixes <- wikidataPrefixes
  } yield Json.fromFields(
    List(
      ("prefixes", prefixes),
      (
        "shape_map",
        Json.fromString(
          "SPARQL'SELECT DISTINCT ?virus WHERE {   VALUES ?virus {  wd:Q82069695  }  }'@<Virus>  "
        )
      ),
      ("endpoint", Json.fromString("https://query.wikidata.org/sparql")),
      ("all_classes", Json.False),
      ("query_depth", Json.fromInt(1)),
      ("threshold", Json.fromInt(0)),
      (
        "instantiation_prop",
        Json.fromString("http://www.wikidata.org/prop/direct/P31")
      ),
      ("disable_comments", Json.True),
      ("shape_qualifiers_mode", Json.True),
      (
        "namespaces_for_qualifiers",
        Json.arr(Json.fromString("http://www.wikidata.org/prop/"))
      )
    )
  )

  private case class InfoEntity(localName: String, uri: Uri, sourceUri: String)

  private def cnvEntity(entity: String): Either[String, InfoEntity] = {
    val wdRegex = "http://www.wikidata.org/entity/(.*)".r
    entity match {
      case wdRegex(localName) => {
        val uri =
          uri"https://www.wikidata.org" / "wiki" / "Special:EntityData" / (localName + ".ttl")
        InfoEntity(localName, uri, entity).asRight[String]
      }
      case _ =>
        s"Entity: $entity doesn't match regular expression: ${wdRegex}"
          .asLeft[InfoEntity]
    }
  }

  private def cnvEntity2(entity: String): Either[String, InfoEntity] = {
    val wdRegex: Regex = "<(http://www.wikidata.org/entity/(.*))>".r
    entity match {
      case wdRegex(_, _) => {
        val matches = wdRegex.findAllIn(entity)
        pprint.log(matches)
        if(matches.groupCount == 2) {
          val localName = matches.group(2)
          val sourceUri = matches.group(1)
          val uri =
            uri"https://www.wikidata.org" / "wiki" / "Special:EntityData" / (localName + ".ttl")
          pprint.log(uri)
          InfoEntity(localName, uri, sourceUri).asRight[String]
        } else
          s"Entity: $entity doesn't match regular expression: ${wdRegex}"
            .asLeft[InfoEntity]
      }
      case _ =>
        s"Entity: $entity doesn't match regular expression: ${wdRegex}"
          .asLeft[InfoEntity]
    }
  }

  private def wdEntity(
      optEntity: Option[String],
      withDot: Boolean
  ): IO[Option[Json]] = {
    optEntity match {
      case None => IO.pure(None)
      case Some(entity) => {
        val process = for {
          uri <- getUri(entity)
//          data <- resolve(uri)
//          rdf <- getRDF(data)
//          maybeDot <- generateDot(rdf, withDot)/* if (generateDot)
/* EitherT.fromEither[F](RDF2Dot.rdf2dot(rdf).bimap(e => s"Error
           * converting to Dot: $e", s => Some(s.toString))) */
//                  else EitherT.pure(none) */
          json <- prepareJson(
            entity,
            proxyUri(uri)
          ) // prepareJsonOk(entity, uri, rdf, maybeDot)
        } yield Option(json)
        process.value.flatMap(e => IO.pure(mkJson(entity, e)))
      }
    }
  }

  private def proxyUri(uri: Uri): Uri = {
    apiUri.withQueryParam("entity", uri.renderString)
  }

  private def mkJson(
      entity: String,
      e: Either[String, Option[Json]]
  ): Option[Json] =
    e.fold(msg => Some(jsonErr(entity, msg)), identity)

  private def getUri(entity: String): EitherT[IO, String, Uri] = {
    println(s"getUri: $entity")
    val q = """Q(\d*)""".r
    entity match {
      case q(n) => EitherT.pure(wikidataEntityUrl / ("Q" + n))
      case _ =>
        EitherT.fromEither[IO](
          Uri
            .fromString(entity)
            .leftMap(f => s"Error creating URI from $entity: $f")
        )
    }
  }

  private def resolve(uri: Uri): EitherT[IO, String, Stream[IO, String]] = {
    println(s"Resolve: $uri")
    for {
      eitherData <- EitherT.liftF(resolveStream[IO](uri, client))
      data <- EitherT.fromEither[IO](
        eitherData.leftMap(e => s"Error retrieving $uri: $e")
      )
    } yield data
  }

  /* private def getRDF(str: Stream[F,String]): EitherT[F, String, RDFReader] =
   * EitherT.liftF(LiftIO[F].liftIO(RDFAsJenaModel.empty)) */

  private def fromIO[A](io: IO[A]): EitherT[IO, String, A] = EitherT.liftF(io)

  private def generateDot(
      rdf: RDFReader,
      maybeDot: Boolean
  ): EitherT[IO, String, Option[String]] =
    if(maybeDot) for {
      sgraph <- fromIO(
        RDF2SGraph.rdf2sgraph(rdf)
      ) // .bimap(e => s"Error converting to Dot: $e", s => Some(s.toString)))
    } yield Option(sgraph.toDot(RDFDotPreferences.defaultRDFPrefs))
    else
      EitherT.pure(None)

  private def jsonErr(entity: String, msg: String): Json =
    Json.fromFields(
      List(
        ("entity", Json.fromString(entity)),
        ("error", Json.fromString(msg))
      )
    )

  private def prepareJson(entity: String, uri: Uri): EitherT[IO, String, Json] =
    EitherT.pure(
      Json.fromFields(
        List(
          ("entity", Json.fromString(entity)),
          ("uri", Json.fromString(uri.toString))
        )
      )
    )

  private def prepareJsonOk(
      entity: String,
      uri: Uri,
      rdf: RDFReader,
      maybeDot: Option[String]
  ): EitherT[IO, String, Json] = for {
    serialized <- fromIO(rdf.serialize("TURTLE"))
  } yield Json.fromFields(
    List(
      ("entity", Json.fromString(entity)),
      ("uri", Json.fromString(uri.toString)),
      ("rdf", Json.fromString(serialized))
    ) ++ dotField(maybeDot)
  )

  private def dotField(maybeDot: Option[String]): List[(String, Json)] =
    maybeDot.fold(List[(String, Json)]())(s =>
      List(("dot", Json.fromString(s)))
    )

  /* optEntity match { case None => Monad[F].pure(None) case Some(entity) => {
   * val q = """Q(\d*)""".r val eitherUri = entity match { case q(n) =>
   * Uri.fromString(wikidataEntityUrl + n) case _ => Uri.fromString(entity) }
   * val result = eitherUri.fold( e => Json.fromString(s"Bad URI ${entity}:
   * $e"), uri => for { data <- client.use {
   * withRedirect(_).expect[String]("http://www.wikidata.org/entity/Q14317") }
   * eitherRDF = RDFAsJenaModel.fromString(data,"TURTLE",None).fold( e =>
   * Monad[F].pure(s"Error parsing RDF: $e\n$data".asLeft[RDFReader]), rdf =>
   * rdf.asRight[String]) _ <- LiftIO[F].liftIO(IO { println(s"Searching
   * wikidata: $data") }) } yield Monad[F].pure(Json.fromFields(List( ("entity",
   * Json.fromString(entity)), ("data", Json.fromString(data)) )))) ) } */

  /* Languages have this structure:
   * { "batchcomplete": "", "query": { "wbcontentlanguages": { "aa": { "code":
   * "aa", "autonym": "Qafár af" }, "ab": { "code": "ab", "autonym": "Аҧсшәа" }
   * } } */

  def cnvEntities(json: Json): Either[String, Json] = for {
    entities <- json.hcursor
      .downField("search")
      .values
      .toRight("Error obtaining search value")
    converted = Json.fromValues(
      entities.map((value: Json) =>
        Json.fromFields(
          List(
            (
              "label",
              value.hcursor.downField("label").focus.getOrElse(Json.Null)
            ),
            ("id", value.hcursor.downField("id").focus.getOrElse(Json.Null)),
            (
              "uri",
              value.hcursor.downField("concepturi").focus.getOrElse(Json.Null)
            ),
            (
              "descr",
              value.hcursor.downField("description").focus.getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield converted

  private def cnvLanguages(json: Json): Either[String, Json] = for {
    // query <- .focus.toRight(s"Error obtaining query at ${json.spaces2}" )
    languagesObj <- json.hcursor
      .downField("query")
      .downField("wbcontentlanguages")
      .focus
      .toRight(s"Error obtaining query/wbcontentlanguages at ${json.spaces2}")
    keys <- languagesObj.hcursor.keys.toRight(
      s"Error obtaining values from languages: ${languagesObj.spaces2}"
    )
    converted = Json.fromValues(
      keys.map(key =>
        Json.fromFields(
          List(
            (
              "label",
              languagesObj.hcursor
                .downField(key)
                .downField("code")
                .focus
                .getOrElse(Json.Null)
            ),
            (
              "name",
              languagesObj.hcursor
                .downField(key)
                .downField("autonym")
                .focus
                .getOrElse(Json.Null)
            )
          )
        )
      )
    )
  } yield {
    converted
  }

  private def cnvEntitySchema(wdSchema: String): Uri = {
    val uri = uri"https://www.wikidata.org".withPath(
      Uri.Path.unsafeFromString(s"/wiki/Special:EntitySchemaText/${wdSchema}")
    )
    uri
  }

}

object WikidataService {
  def apply(client: Client[IO]): WikidataService =
    new WikidataService(client)
}
