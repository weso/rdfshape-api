package es.weso.server

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import cats.implicits._
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Host, Location}
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.typelevel.ci._

import java.io.{FileInputStream, IOException}
import java.nio.file.Paths
import java.security.{KeyStore, SecureRandom, Security}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.language.higherKinds

object SSLHelper {
  val keyStorePassword: String   = sys.env.getOrElse("KEYSTORE_PASSWORD", "")
  val keyManagerPassword: String = sys.env.getOrElse("KEYMANAGER_PASSWORD", "")
  val keyStorePath: String =
    Paths.get(sys.env.getOrElse("KEYSTORE_PATH", "")).toAbsolutePath.toString
  val storeInfo: StoreInfo = StoreInfo(keyStorePath, keyStorePassword)

  def getContext: SSLContext = {
    if(
      keyStorePassword == "" || keyManagerPassword == "" || keyStorePath == ""
    ) {
      SSLContext.getDefault
    } else {
      try {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)

        val in = new FileInputStream(keyStorePath)
        keyStore.load(in, keyStorePassword.toCharArray)

        val keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray)

        val trustManagerFactory = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm
        )
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
          keyManagerFactory.getKeyManagers,
          trustManagerFactory.getTrustManagers,
          new SecureRandom()
        )
        sslContext
      } catch {
        case _: IOException => SSLContext.getDefault
      }
    }
  }
}
