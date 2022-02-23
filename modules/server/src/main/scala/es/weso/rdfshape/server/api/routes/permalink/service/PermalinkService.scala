package es.weso.rdfshape.server.api.routes.permalink.service

import cats.effect._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.api
import es.weso.rdfshape.server.api.routes.ApiService
import es.weso.rdfshape.server.api.routes.permalink.logic.Permalink
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  UrlCodeParameter,
  UrlParameter
}
import es.weso.rdfshape.server.implicits.query_parsers.urlQueryParser
import mongo4cats.circe._
import mongo4cats.client.MongoClient
import mongo4cats.collection.operations.{Filter, Update}
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.RhoRoutes

import java.net.URL

/** API endpoint to handle the permalink service (creation, retrieval, etc.)
  *
  * @param client HTTP4S client object
  */
class PermalinkService(client: Client[IO])
    extends Http4sDsl[IO]
    with ApiService
    with LazyLogging {

  /** Database user. Can be overridden by environment variables.
    */
  private lazy val dbUserName =
    sys.env.getOrElse("MONGO_USER", "rdfshape-user")

  /** Database password. Can be overridden by environment variables.
    */
  private lazy val dbPassword =
    sys.env.getOrElse("MONGO_PASSWORD", "rdfshape-user")

  /** Database name. Can be overridden by environment variables.
    */
  private lazy val dbName =
    sys.env.getOrElse("MONGO_DATABASE", "rdfshape")

  /** Database collection name. Can be overridden by environment variables.
    */
  private lazy val collectionName =
    sys.env.getOrElse("MONGO_COLLECTION", "permalinks")

  /** Final connection String formed by interpolating database credentials.
    */
  private lazy val connectionString =
    s"mongodb+srv://$dbUserName:$dbPassword@cluster0.pnja6.mongodb.net/$dbName" +
      "?retryWrites=true&w=majority"

  private lazy val dbClient =
    MongoClient.fromConnectionString[IO](connectionString)

  override val verb: String = "permalink"

  /** Describe the API routes handled by this service and the actions performed on each of them
    */
  val routes: RhoRoutes[IO] = new RhoRoutes[IO] {

    /** Create a permalink to another resource.
      * Receives the URL to be linked:
      *  - url [String]: URL to be linked
      *    Returns the permalink unique code in the response body
      *
      * @note QueryParser already checks for the validity of the URL
      */
    GET / `api` / `verb` / "generate" +? param[URL](UrlParameter.name) |>> {
      (urlObj: URL) =>
        // Extract the relevant info from the URL
        val urlPath = extractUrlPathAndQuery(urlObj)
        for {
          // Check if the permalink exists
          optExistingPermalink <- getPermalinkByUrl(urlPath)
          response <- optExistingPermalink match {
            // Exists, return its code
            case Some(pm) =>
              Ok(pm.urlCode.toString)
            // Does not exist, create it
            case None =>
              // Create permalink model object
              val newPermalink = Permalink(longUrl = urlPath)
              insertPermalink(newPermalink).flatMap {
                // Inserted, return code
                case Some(pm) => Created(pm.urlCode.toString)
                // Not inserted, return generic error
                case None =>
                  InternalServerError(
                    s"Could not generate the permalink for url: $urlObj"
                  )
              }
          }
        } yield response
    }

    /** Retrieve a URL to a resource given its permalink code.
      * Receives the permalink code to be checked:
      *  - urlCode [Long]: code to be checked
      *    Returns the permalink target (if present in the database) in the response body
      */
    GET / `api` / `verb` / "get" +? param[Long](UrlCodeParameter.name) |>> {
      urlCode: Long =>
        // Fetch document in database
        for {
          optExistingPermalink <- getPermalinkByCode(urlCode)
          response <- optExistingPermalink match {
            // Exists, return its code
            case Some(pm) =>
              logger.info(
                s"Retrieved original url: ${pm.urlCode} => ${pm.longUrl}"
              )
              Ok(pm.longUrl)
            // Does not exist, return error
            case None =>
              val msg = s"Could not find the permalink with code: $urlCode"
              logger.warn(msg)
              NotFound(msg)
          }
        } yield response
    }
  }

  /** @param url URL object used as input
    * @return A String containing the query path followed by the query string, if any
    * @example http://google.com/es => /es
    * @example http://google.com/es?param=value => /es?param=value
    */
  private def extractUrlPathAndQuery(url: URL): String = {
    Option(url.getQuery) match {
      case Some(queryString) => s"${url.getPath}?$queryString"
      case None              => url.getPath
    }
  }

  /** Given a possible permalink code, get its corresponding permalink
    *
    * @param urlCode Permalink code for which we want to find the permalink
    * @return Optionally, the Permalink with the given code
    */
  private def getPermalinkByCode(urlCode: Long): IO[Option[Permalink]] = {
    logger.debug(s"Retrieve permalink with code: $urlCode")
    val codeFilter = Filter.eq("urlCode", urlCode)
    retrievePermalink(codeFilter)
  }

  /** Given a condition, search for a permalink fulfilling it.
    *
    * @param filter     Filter used to to find the permalink
    * @param updateDate Whether to update the access-date of the permalink when
    *                   retrieved to mark its last usage
    * @return Optionally, the Permalink already fulfilling the condition
    */
  private def retrievePermalink(
      filter: Filter,
      updateDate: Boolean = true
  ): IO[Option[Permalink]] = {
    // Connect to check for already existing links to the target
    dbClient
      .use(client => {
        for {
          db                <- client.getDatabase(dbName)
          col               <- db.getCollectionWithCodec[Permalink](collectionName)
          existingPermalink <- col.find(filter).first
        } yield existingPermalink match {
          // Refresh the use date of the link if found
          case Some(permalink) if updateDate => updatePermalinkUsage(permalink)
          case Some(_) | None                => IO.pure(existingPermalink)
        }
      })
      .flatten
  }

  /** Update the access date of a given permalink (invoked when it is accessed)
    *
    * @param permalink Permalink to be updated
    */
  private def updatePermalinkUsage(
      permalink: Permalink
  ): IO[Option[Permalink]] = {
    logger.debug(s"Permalink update: $permalink")

    // Create filter to query the database
    val sameCodeFilter = Filter.eq("urlCode", permalink.urlCode)
    // Create update to be applied
    val usageDateUpdate = Update.currentDate("date")

    // Connect to update matching links
    dbClient
      .use(client => {
        for {
          db     <- client.getDatabase(dbName)
          col    <- db.getCollectionWithCodec[Permalink](collectionName)
          update <- col.findOneAndUpdate(sameCodeFilter, usageDateUpdate)
        } yield update
      })
  }

  /** Given the path of a URL, get the permalink already targeting it, if any
    *
    * @param urlPath URL which we want to find a permalink for
    * @return Optionally, the Permalink targeting the given URL
    */
  private def getPermalinkByUrl(urlPath: String): IO[Option[Permalink]] = {
    logger.debug(s"Retrieve permalink for URL: $urlPath")
    val sameUrlPathFilter = Filter.eq("longUrl", urlPath)
    retrievePermalink(sameUrlPathFilter)
  }

  /** Insert a given permalink in the database
    *
    * @param permalink Permalink to be inserted
    */
  private def insertPermalink(
      permalink: Permalink
  ): IO[Option[Permalink]] = {
    logger.debug(s"Permalink insert: $permalink")

    // Connect and insert
    dbClient
      .use(client => {
        for {
          db        <- client.getDatabase(dbName)
          col       <- db.getCollectionWithCodec[Permalink](collectionName)
          insertion <- col.insertOne(permalink)
        } yield
          if(insertion.wasAcknowledged) {
            Some(permalink)
          } else None
      })
  }
}

object PermalinkService {

  /** Service factory
    *
    * @param client Underlying http4s client
    * @return A new Permalink Service
    */
  def apply(client: Client[IO]): PermalinkService = new PermalinkService(client)
}
