package es.weso.rdfshape.server.api

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import org.http4s.rho.RhoMiddleware
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerMetadata}

package object swagger {

  /** Swagger middleware to transform RhoServices into HttpServices
    * with an attached Swagger definition
    * Includes the base API spec
    * @note We use the default API paths; therefore swagger files are in
    *       /swagger.(json|yml)
    */
  lazy val swaggerMiddleware: RhoMiddleware[IO] =
    ioSwagger.createRhoMiddleware(
      swaggerFormats = DefaultSwaggerFormats,
      swaggerMetadata = SwaggerMetadata(
        apiInfo = Info(
          title = "RDFShape API",
          version =
            buildinfo.BuildInfo.version, // programmatically get project version
          description =
            ("RDFShape is web API for semantic data analysis and validation" +
              "implemented in Scala using http4s (https://http4s.org/)").some,
          contact = Contact(
            name = "WESO Research group",
            email = "info@weso.es".some,
            url = "https://www.weso.es/#contact".some
          ).some,
          license = License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
          ).some
        ),
        host = "api.rdfshape.weso.es".some,
        basePath = "/api/".some,
        schemes = List(Scheme.HTTP, Scheme.HTTPS),
        consumes = List("multipart/form-data"),
        produces = List("text/plain; charset=utf-8", "application/json"),
        tags = List(Tag(name = "api", description = "RDFShape REST API".some))
      )
    )
}
