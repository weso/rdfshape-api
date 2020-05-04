package es.weso.server
import cats._
import cats.effect.{Effect, IO}
import data._
import implicits._
import org.http4s.multipart.Part
import fs2.text.utf8Decode

case class PartsMap[F[_]: Effect] private(map: Map[String,Part[F]]) {

  def eitherPartValue(key: String): F[Either[String,String]] = for {
    maybeValue <- optPartValue(key)
  } yield maybeValue match {
    case None => Left(s"Not found value for key $key\nKeys available: ${map.keySet.mkString(",")}")
    case Some(s) => Right(s)
  }

  def optPartValue(key: String): F[Option[String]] =
    map.get(key) match {
      case Some(part) =>
        part.body.through(utf8Decode).compile.foldMonoid.map(Some.apply)
      case None => Effect[F].point(None)
    }

  def optPartValueBoolean(key: String): F[Option[Boolean]] = map.get(key) match {
    case Some(part) => part.body.through(utf8Decode).compile.foldMonoid.map {
      case "true" => Some(true)
      case "false" => Some(false)
      case _ => None
    }
    case None => Effect[F].point(None)
  }

  def partValue(key:String): F[String] = for {
    eitherValue <- eitherPartValue(key)
    value <- eitherValue.fold(
      s => MonadError[F,Throwable].raiseError(new RuntimeException(s)), 
      Monad[F].pure(_))
  } yield value
}

object PartsMap {

  def apply[F[_]:Effect](ps: Vector[Part[F]]): PartsMap[F] = {
    PartsMap(ps.filter(_.name.isDefined).map(p => (p.name.get,p)).toMap)
  }

}