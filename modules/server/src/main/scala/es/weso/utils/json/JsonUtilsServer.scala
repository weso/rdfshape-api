package es.weso.utils.json

import io.circe.Json

object JsonUtilsServer {
  def maybeField[A](
      data: Option[A],
      name: String,
      cnv: A => Json
  ): List[(String, Json)] =
    data match {
      case None    => List()
      case Some(v) => List((name, cnv(v)))
    }

  def eitherField[A](
      data: Either[String, A],
      name: String,
      cnv: A => Json
  ): List[(String, Json)] =
    data match {
      case Left(msg) => List((name, Json.fromString(msg)))
      case Right(v)  => List((name, cnv(v)))
    }

}
