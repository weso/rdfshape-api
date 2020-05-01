package es.weso.server

import cats.effect.{IO, Sync}
import cats.implicits._
import java.nio.file.Paths
import java.security.{KeyStore, Security}

import javax.net.ssl.{KeyManagerFactory, SSLContext}
import org.http4s.HttpApp
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Host, Location}
import org.http4s.server.SSLKeyStoreSupport.StoreInfo

object SSLHelper {
  val keystorePassword: String = "wesopassword"
  val keyManagerPassword: String = ""

  // val keystorePath: String = Paths.get("../server.jks").toAbsolutePath.toString

  // val storeInfo: StoreInfo = StoreInfo(keystorePath, keystorePassword)
  def handler(e: Throwable): IO[SSLContext] = for {
    _ <- IO( println(s"Exception: $e"))
  } yield SSLContext.getDefault

  def loadContextFromClasspath[F[_]](keystorePassword: String, keyManagerPass: String)(
    implicit F: Sync[F]): F[SSLContext] =
    F.delay {
      val ksStream = this.getClass.getResourceAsStream("/server.jks")
      val ks = KeyStore.getInstance("JKS")
      ks.load(ksStream, keystorePassword.toCharArray)

      println(s"KeyStore loaded: $ks")

      ksStream.close()

      val kmf = KeyManagerFactory.getInstance(
        Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
          .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

      println(s"kmf before init: $kmf")

      kmf.init(ks, keyManagerPass.toCharArray)


      println(s"kmf after init: $kmf")

      val context = SSLContext.getInstance("TLS")

      println(s"context before init: $context")

      context.init(kmf.getKeyManagers, null, null)

      println(s"context after init: $context")

      context
    }

  def redirectApp[F[_]: Sync](securePort: Int): HttpApp[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpApp[F] { request =>
      request.headers.get(Host) match {
        case Some(Host(host @ _, _)) =>
          val baseUri = request.uri.copy(
            scheme = Scheme.https.some,
            authority = Some(
              Authority(
                userInfo = request.uri.authority.flatMap(_.userInfo),
                host = RegName(host),
                port = securePort.some)))
          MovedPermanently(Location(baseUri.withPath(request.uri.path)))
        case _ =>
          BadRequest()
      }
    }
  }
}