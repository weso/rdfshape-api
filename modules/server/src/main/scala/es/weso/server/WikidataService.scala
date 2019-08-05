package es.weso.server
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import es.weso._
import es.weso.rdf.RDFReader
import es.weso.rdf.dot.RDF2Dot
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.server.QueryParams.{OptEntityParam, OptSchemaParam, OptWithDotParam}
import es.weso.server.values._
import org.http4s._
import org.http4s.twirl._
import io.circe._
import fs2.{io => ioFs2, _}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{FollowRedirect, Logger}
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart
import es.weso.server.utils.Http4sUtils._
import scala.concurrent.ExecutionContext.global

class WikidataService[F[_]: ConcurrentEffect](blocker: Blocker)(implicit F: Effect[F], cs: ContextShift[F])
  extends Http4sDsl[F] {

  implicit val client = BlazeClientBuilder[F](global).resource

  val wikidataEntityUrl = "http://www.wikidata.org/entity/Q"

  def routes(implicit timer: Timer[F]): HttpRoutes[F] = HttpRoutes.of[F] {

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
          data <- resolve(uri)
          rdf <- getRDF(data)
          maybeDot <- generateDot(rdf, withDot)/* if (generateDot)
                   EitherT.fromEither[F](RDF2Dot.rdf2dot(rdf).bimap(e => s"Error converting to Dot: $e", s => Some(s.toString)))
                  else EitherT.pure(none) */
          json <- prepareJsonOk(entity, uri, rdf, maybeDot)
        } yield Option(json)
        process.value.flatMap(e => F.pure(mkJson(entity,e)))
      }
    }
  }

  private def mkJson(entity: String, e: Either[String, Option[Json]]): Option[Json] =
    e.fold(msg => Some(jsonErr(entity, msg)), identity)

  private def getUri(entity: String): EitherT[F,String, Uri] = {
   println(s"getUri: $entity")
   val q = """Q(\d*)""".r
   entity match {
    case q(n) => EitherT.fromEither[F](Uri.fromString(wikidataEntityUrl + n).leftMap(f => s"Error creating URI for entity ${n}: ${f}"))
    case _ => EitherT.fromEither[F](Uri.fromString(entity).leftMap(f => s"Error creating URI from $entity: $f"))
   }
  }

  private def resolve(uri: Uri): EitherT[F, String, Stream[F,String]] = {
    println(s"Resolve: $uri")
    for {
      eitherData <- EitherT.liftF(resolveStream[F](uri))
      data <- EitherT.fromEither[F](eitherData.leftMap(e => s"Error retrieving $uri: $e"))
    } yield data
  }

  private def getRDF(str: Stream[F,String]): EitherT[F, String, RDFReader] = EitherT.pure(RDFAsJenaModel.empty)
  /* for {
    ls <-EitherT.liftF(str.compile.toList)
    rdf <- EitherT.fromEither[F](RDFAsJenaModel.fromString(ls.mkString, "TURTLE", None))
  } yield rdf */

  private def generateDot(rdf: RDFReader, maybeDot: Boolean): EitherT[F, String, Option[String]] =
    if (maybeDot) {
      EitherT.fromEither[F](RDF2Dot.rdf2dot(rdf).bimap(e => s"Error converting to Dot: $e", s => Some(s.toString)))
    }
    else
      EitherT.pure(None)

  private def jsonErr(entity: String, msg: String): Json =
    Json.fromFields(List(
    ("entity", Json.fromString(entity)),
    ("msg", Json.fromString(msg))
    ))

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
  def apply[F[_]: Effect: ConcurrentEffect: ContextShift](blocker: Blocker): WikidataService[F] =
    new WikidataService[F](blocker)
}

