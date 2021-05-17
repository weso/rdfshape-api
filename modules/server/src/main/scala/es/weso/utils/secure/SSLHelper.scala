package es.weso.utils.secure

import org.http4s.server.SSLKeyStoreSupport.StoreInfo

import java.io.{FileInputStream, IOException}
import java.nio.file.Paths
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

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
