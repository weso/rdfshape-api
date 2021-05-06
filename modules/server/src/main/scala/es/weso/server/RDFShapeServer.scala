package es.weso.server
import cats.effect._

import scala.util.Properties.envOrNone

object RDFShapeServer extends IOApp {

  private val ip   = "0.0.0.0"
  private val port = envOrNone("PORT") map (_.toInt) getOrElse 8080
  println(s"PORT: $port")

  override def run(args: List[String]): IO[ExitCode] = {
    Server.stream(port, ip).compile.drain.as(ExitCode.Success)
  }

}
