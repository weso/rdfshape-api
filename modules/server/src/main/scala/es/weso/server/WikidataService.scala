package es.weso.server

import cats.data._
import cats.effect._
import cats.implicits._
import es.weso._
import es.weso.rdf.RDFReader
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.streams.Streams
import es.weso.server.QueryParams.{ContinueParam, LabelParam, LanguageParam, LimitParam, OptEntityParam, OptWithDotParam, SchemaEngineParam, WdEntityParam}
import es.weso.server.utils.Http4sUtils._
import es.weso.server.values._
import io.circe._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Accept
import org.http4s.multipart.Multipart
import org.http4s.twirl._
import org.http4s.implicits._
import es.weso.rdf.sgraph._
import APIDefinitions._

class WikidataService[F[_]: ConcurrentEffect](blocker: Blocker,
                                              client: Client[F]
                                             )(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  val wikidataEntityUrl = uri"http://www.wikidata.org/entity"
  val apiUri = uri"/api/wikidata/entity"
  val defaultLimit = 20
  val defaultContinue = 1


  def routes(implicit timer: Timer[F]): HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / `api` / "wikidata" / "test"  => {
      Ok("Wikidata Test")
    }

    case GET -> Root / `api` / "wikidata" / "entityLabel" :?
       WdEntityParam(entity) +&
       LanguageParam(language) => {
        val uri = Uri.unsafeFromString(s"https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels&ids=${entity}&languages=${language}&format=json")
        val req: Request[F] = Request(method = GET, uri = uri)
         for {
          either <- client.fetch(req) {
            case Status.Successful(r) => r.attemptAs[Json].leftMap(_.message).value
            case r => r.as[String].map(b => s"Request $req failed with status ${r.status.code} and body $b".asLeft[Json])
          }
          resp <- Ok(either.fold(Json.fromString(_), identity))
         } yield resp
    }

    case GET -> Root / `api` / "wikidata" / "searchEntity" :?
      LabelParam(label) +&
      LanguageParam(language) +&
      LimitParam(maybelimit) +&
      ContinueParam(maybeContinue) => {
      val limit: String = maybelimit.getOrElse(defaultLimit.toString)
      val continue: String = maybeContinue.getOrElse(defaultContinue.toString)
      val uri = uri"https://www.wikidata.org".
        withPath("/w/api.php").
        withQueryParam("action", "wbsearchentities").
        withQueryParam("search", label).
        withQueryParam("language", language).
        withQueryParam("limit",limit).
        withQueryParam("continue",continue).
        withQueryParam("format","json")

      val req: Request[F] = Request(method = GET, uri = uri)
      for {
        either <- client.fetch(req) {
          case Status.Successful(r) => r.attemptAs[Json].leftMap(_.message).value
          case r => r.as[String].map(b => s"Request $req failed with status ${r.status.code} and body $b".asLeft[Json])
        }
        resp <- Ok(either.fold(Json.fromString(_), identity))
      } yield resp
    }

/*    case GET -> Root / "testQ" => {
      val req: Request[F] = Request(uri = wikidataEntityUrl / "Q33").withHeaders(Accept(MediaType.text.turtle))
      client.toHttpApp(req).flatMap(resp => Ok(Streams.cnv(resp.body)))
    } */

    case GET -> Root / "testR" => {
      // val req: Request[F] = Request(uri = wikidataEntityUrl / "Q33").withHeaders(Accept(MediaType.text.turtle))
      Ok(Streams.getRaw(wikidataEntityUrl / "Q33"))
    }

    case GET -> Root / "testRM" => {
      // val req: Request[F] = Request(uri = wikidataEntityUrl / "Q33").withHeaders(Accept(MediaType.text.turtle))
      Ok(Streams.getRawWithModel(wikidataEntityUrl / "Q33"))
    }


    case GET -> Root / "wdEntity" :?
      OptEntityParam(optEntity) +&
      OptWithDotParam(optWithDot) => {
      val maybeDot = optWithDot.fold(false)(identity)
      for {
        result <- wdEntity(optEntity, maybeDot)
        response <- Ok(html.wdEntity(result, WikidataEntityValue(optEntity)))
      } yield response
    }

    case req@POST -> Root / "wdEntity" => req.decode[Multipart[F]] { m => {
      val partsMap = PartsMap(m.parts)
      for {
        optEntity <- partsMap.optPartValue("entity")
        optWithDot <- partsMap.optPartValue("withDot")
        result <- {
          val withDot = optWithDot.fold(false)(_.toLowerCase match {
            case "false" => false
            case "true" => true
            case _ => false
          })
          wdEntity(optEntity, withDot)
        }
        response <- Ok(html.wdEntity(result, WikidataEntityValue(optEntity)))
      } yield response
    }
    }

    case req@GET -> Root / "wdSchema" => Ok("Not implemented wikidata schema yet")
    case req@GET -> Root / "wdValidate" => Ok("Not implemented wikidata validate yet")
    case req@GET -> Root / "wdExtract" => Ok("Not implemented wikidata extract yet")
  }

  private def wdEntity(optEntity: Option[String], withDot: Boolean): F[Option[Json]] = {
    optEntity match {
      case None => F.pure(None)
      case Some(entity) => {
        val process = for {
          uri <- getUri(entity)
//          data <- resolve(uri)
//          rdf <- getRDF(data)
//          maybeDot <- generateDot(rdf, withDot)/* if (generateDot)
//                   EitherT.fromEither[F](RDF2Dot.rdf2dot(rdf).bimap(e => s"Error converting to Dot: $e", s => Some(s.toString)))
//                  else EitherT.pure(none) */
          json <- prepareJson(entity, proxyUri(uri))// prepareJsonOk(entity, uri, rdf, maybeDot)
        } yield Option(json)
        process.value.flatMap(e => F.pure(mkJson(entity,e)))
      }
    }
  }

  private def proxyUri(uri: Uri): Uri = {
    apiUri.withQueryParam("entity",uri.renderString)
  }

  private def mkJson(entity: String, e: Either[String, Option[Json]]): Option[Json] =
    e.fold(msg => Some(jsonErr(entity, msg)), identity)

  private def getUri(entity: String): EitherT[F,String, Uri] = {
   println(s"getUri: $entity")
   val q = """Q(\d*)""".r
   entity match {
    case q(n) => EitherT.pure(wikidataEntityUrl / ("Q" + n))
    case _ => EitherT.fromEither[F](Uri.fromString(entity).leftMap(f => s"Error creating URI from $entity: $f"))
   }
  }

  private def resolve(uri: Uri): EitherT[F, String, Stream[F,String]] = {
    println(s"Resolve: $uri")
    for {
      eitherData <- EitherT.liftF(resolveStream[F](uri, client))
      data <- EitherT.fromEither[F](eitherData.leftMap(e => s"Error retrieving $uri: $e"))
    } yield data
  }

  private def getRDF(str: Stream[F,String]): EitherT[F, String, RDFReader] = EitherT.pure(RDFAsJenaModel.empty)
  /* for {
    ls <-EitherT.liftF(str.compile.toList)
    rdf <- EitherT.fromEither[F](RDFAsJenaModel.fromString(ls.mkString, "TURTLE", None))
  } yield rdf */

  private def generateDot(rdf: RDFReader, maybeDot: Boolean): EitherT[F, String, Option[String]] =
    if (maybeDot) for {
      sgraph <- EitherT.fromEither[F](RDF2SGraph.rdf2sgraph(rdf))  // .bimap(e => s"Error converting to Dot: $e", s => Some(s.toString)))
    } yield Option(sgraph.toDot(RDFDotPreferences.defaultRDFPrefs))
    else
      EitherT.pure(None)

  private def jsonErr(entity: String, msg: String): Json =
    Json.fromFields(List(
    ("entity", Json.fromString(entity)),
    ("error", Json.fromString(msg))
    ))

  private def prepareJson(entity: String,
                            uri: Uri
                           ): EitherT[F, String, Json] =
   EitherT.pure(Json.fromFields(List(
    ("entity", Json.fromString(entity)),
    ("uri", Json.fromString(uri.toString)),
   )))

  private def prepareJsonOk(entity: String,
                          uri: Uri,
                          rdf: RDFReader,
                          maybeDot: Option[String]
                         ): EitherT[F, String, Json] = for {
    serialized <- EitherT.fromEither[F](rdf.serialize("TURTLE"))
  } yield Json.fromFields(List(
    ("entity", Json.fromString(entity)),
    ("uri", Json.fromString(uri.toString)),
    ("rdf", Json.fromString(serialized))
   ) ++ dotField(maybeDot)
  )

  private def dotField(maybeDot: Option[String]): List[(String,Json)] =
    maybeDot.fold(List[(String,Json)]())(s => List(("dot",Json.fromString(s))))

/*    optEntity match {
      case None => Monad[F].pure(None)
      case Some(entity) => {
        val q = """Q(\d*)""".r
        val eitherUri = entity match {
          case q(n) => Uri.fromString(wikidataEntityUrl + n)
          case _ => Uri.fromString(entity)
        }
        val result = eitherUri.fold(
          e => Json.fromString(s"Bad URI ${entity}: $e"),
          uri => for {
            data <- client.use { withRedirect(_).expect[String]("http://www.wikidata.org/entity/Q14317") }
            eitherRDF = RDFAsJenaModel.fromString(data,"TURTLE",None).fold(
              e => Monad[F].pure(s"Error parsing RDF: $e\n$data".asLeft[RDFReader]),
              rdf => rdf.asRight[String])
            _ <- LiftIO[F].liftIO(IO { println(s"Searching wikidata: $data") })
          } yield Monad[F].pure(Json.fromFields(List(
            ("entity", Json.fromString(entity)),
            ("data", Json.fromString(data))
          ))))
        )
      } */

}

object WikidataService {
  def apply[F[_]: Effect: ConcurrentEffect: ContextShift](blocker: Blocker,
                                                          client: Client[F]
                                                         ): WikidataService[F] =
    new WikidataService[F](blocker, client)
}

