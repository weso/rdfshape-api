package es.weso.rdfshape.server.api.routes.permalink.logic

import es.weso.rdfshape.server.utils.numeric.NumericUtils.getRandomInt
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}
import mongo4cats.bson.ObjectId
import mongo4cats.circe._

import java.time.Instant

/** Data class representing a permalink
  *
  * @param _id     Permalink ID (mongo)
  * @param longUrl Permalink target (only the path of the target URL)
  * @param urlCode    Permalink identifying code
  * @param date    Date of last usage of the permalink, defaults to its creation date
  */
final case class Permalink(
    _id: ObjectId = ObjectId(),
    longUrl: String,
    // Time based code
    urlCode: Long = s"${Instant.now.getEpochSecond}${getRandomInt()}".toLong,
    date: Instant = Instant.now
) {

  override def toString: String =
    s"Link '$urlCode' for '$longUrl' (used '$date')"
}

/** Companion object for [[Permalink]] including custom circe codecs
  */
private[permalink] object Permalink {
  // Forcibly implement codecs so that they are in implicit scope later
  implicit val encoder: Encoder[Permalink] = (pm: Permalink) =>
    Json.obj(
      ("_id", pm._id.asJson),
      ("longUrl", pm.longUrl.asJson),
      ("urlCode", pm.urlCode.asJson),
      ("date", pm.date.asJson)
    )
  implicit val decoder: Decoder[Permalink] = (cursor: HCursor) =>
    for {
      id      <- cursor.downField("_id").as[ObjectId]
      longUrl <- cursor.downField("longUrl").as[String]
      urlCode <- cursor.downField("urlCode").as[Long]
      date    <- cursor.downField("date").as[Instant]

      decoded = Permalink(
        id,
        longUrl,
        urlCode,
        date
      )
    } yield decoded
}
