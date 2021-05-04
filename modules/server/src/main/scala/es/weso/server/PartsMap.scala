package es.weso.server
import cats.effect.IO
import cats.implicits._
import fs2.text.utf8Decode
import org.http4s.multipart.Part

case class PartsMap private (map: Map[String, Part[IO]]) {

  def optPartValueBoolean(key: String): IO[Option[Boolean]] =
    map.get(key) match {
      case Some(part) =>
        part.body.through(utf8Decode).compile.foldMonoid.map {
          case "true"  => Some(true)
          case "false" => Some(false)
          case _       => None
        }
      case None => IO.pure(None)
    }

  def partValue(key: String): IO[String] = for {
    eitherValue <- eitherPartValue(key)
    value <- eitherValue.fold(
      s => IO.raiseError(new RuntimeException(s)),
      IO.pure
    )
  } yield value

  def eitherPartValue(key: String): IO[Either[String, String]] = for {
    maybeValue <- optPartValue(key)
  } yield maybeValue match {
    case None =>
      Left(
        s"Not found value for key $key\nKeys available: ${map.keySet.mkString(",")}"
      )
    case Some(s) => Right(s)
  }

  def optPartValue(key: String): IO[Option[String]] =
    map.get(key) match {
      case Some(part) =>
        part.body.through(utf8Decode).compile.foldMonoid.map(Some.apply)
      case None => IO.pure(None)
    }
}

object PartsMap {

  def apply(ps: Vector[Part[IO]]): PartsMap = {
    PartsMap(ps.filter(_.name.isDefined).map(p => (p.name.get, p)).toMap)
  }

}
