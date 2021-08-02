package es.weso.rdfshape.server.utils.secure

import es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException

import java.io.{FileInputStream, IOException}
import java.nio.file.Paths
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

/** Static utilities for creating SSL Contexts to serve the API via HTTPS.
  * Pre-requisites:
  *  - A valid certificate is expected to be found in a keystore.
  *  - Some environment variables need to be set beforehand:
  *     - KEYSTORE_PATH: location of the keystore storing the certificate.
  *     - KEYSTORE_PASSWORD: password protecting the keystore (leave empty if there is none).
  *     - KEYMANAGER_PASSWORD: password protecting the certificate (leave empty is there is none).
  *
  * @note The inner functionality needs to be able read the host's environment and filesystem
  * @note Further docs, see https://github.com/weso/rdfshape-api/wiki/Deploying-RDFShape-API-(SBT)#serving-with-https
  * @see {@link es.weso.rdfshape.server.Server}
  */
object SSLHelper {

  /** Password protecting the keystore, extracted from the host's environment.
    */
  lazy val keyStorePassword: Option[String] = sys.env.get("KEYSTORE_PASSWORD")

  /** Password protecting the certificate, extracted from the host's environment.
    */
  lazy val keyManagerPassword: Option[String] =
    sys.env.get("KEYMANAGER_PASSWORD")

  /** Location of the keystore storing the certificate, extracted from the host's environment.
    */
  lazy val keyStorePath: Option[String] = sys.env.get("KEYSTORE_PATH")

  /** Try to build an SSL Context given that the certificate's location and credentials are in the PATH.
    * @throws es.weso.rdfshape.server.utils.error.exceptions.SSLContextCreationException On errors getting the certificate information
    * @throws java.io.IOException On errors accessing the filesystem
    * @return An SSLContext created from the user's certificate
    */
  @throws(classOf[SSLContextCreationException])
  @throws(classOf[IOException])
  def getContext: SSLContext = {

    if(
      keyStorePassword.isEmpty ||
      keyManagerPassword.isEmpty ||
      keyStorePath.isEmpty
    ) {
      throw SSLContextCreationException(
        "Some environment variables are missing."
      )
    }

    val keyStore            = loadKeystore(keyStorePassword.get)
    val keyManagerFactory   = getKeyManager(keyStore, keyStorePassword.get)
    val trustManagerFactory = getTrustManager(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(
      keyManagerFactory.getKeyManagers,
      trustManagerFactory.getTrustManagers,
      new SecureRandom()
    )
    sslContext
  }

  private def loadKeystore(keyStorePassword: String): KeyStore = {
    val in = new FileInputStream(
      Paths.get(keyStorePath.get).toAbsolutePath.toString
    )
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(in, keyStorePassword.toCharArray)
    keyStore
  }

  private def getKeyManager(keyStore: KeyStore, keyStorePassword: String) = {
    val keyManagerFactory =
      KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(keyStore, keyStorePassword.toCharArray)
    keyManagerFactory
  }

  private def getTrustManager(keyStore: KeyStore) = {
    val trustManagerFactory = TrustManagerFactory.getInstance(
      TrustManagerFactory.getDefaultAlgorithm
    )
    trustManagerFactory.init(keyStore)
    trustManagerFactory
  }
}
