package es.weso.quickstart

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

/* Commented to avoid 2 main apps
object QuickStartMain extends IOApp {

  def main(args: List[String]): Unit = {
    run(args).unsafeRunSync()
  }

  def run(args: List[String]) =
    TestServer.stream[IO].compile.drain.as(ExitCode.Success)
}
*/