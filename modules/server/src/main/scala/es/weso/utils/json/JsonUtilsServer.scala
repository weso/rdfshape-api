package es.weso.utils.json

import io.circe.Json

object JsonUtilsServer {
  def maybeField[A](data: Option[A],
                    name: String,
                    cnv: A => Json
                   ): List[(String,Json)] =
    data match {
      case None => List()
      case Some(v) => List((name,cnv(v)))
    }

}