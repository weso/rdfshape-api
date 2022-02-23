package es.weso.rdfshape.server.api

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.SparqlQuery
import es.weso.rdfshape.server.api.swagger.SwaggerModelProperties.url
import org.http4s.rho.RhoMiddleware
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerMetadata}

import java.net.URL
import scala.reflect.runtime.universe.typeOf

package object swagger {
  // Import all models

  import es.weso.rdfshape.server.api.swagger.SwaggerModels._

  /** Swagger middleware to transform RhoServices into HttpServices
    * with an attached Swagger definition
    * Includes the base API spec
    *
    * All swagger serializers in [[customSwaggerSerializers]] are added to the middleware
    * to create better definitions for complex objects
    *
    * @note We use the default API paths; therefore swagger files are in
    *       /swagger.(json|yml)
    */
  lazy val swaggerMiddleware: RhoMiddleware[IO] =
    ioSwagger.createRhoMiddleware(
      swaggerFormats = allCustomSerializers.foldLeft(
        DefaultSwaggerFormats
      )((prev, curr) =>
        curr match {
          case (classType, serializerModel) =>
            serializerModel match {
              case model: Set[Model] => prev.withSerializers(classType, model)
              case prop: Property    => prev.withFieldSerializers(classType, prop)
            }
        }
      ),
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
        basePath = None,
        schemes = List(Scheme.HTTP, Scheme.HTTPS),
        consumes = List("multipart/form-data"),
        produces = List("text/plain; charset=utf-8", "application/json"),
        tags = List(Tag(name = "api", description = "RDFShape REST API".some))
      )
    )

  /** Merge of both object and field serializer sets
    */
  lazy val allCustomSerializers =
    customSwaggerSerializers ++ customSwaggerFieldSerializers

  /** Set of all swagger object serializers defined in [[SwaggerModels]]
    */
  val customSwaggerSerializers = Set(
    (typeOf[SparqlQuery], sparqlQuery)
  )

  /** Set of all swagger field serializers defined in [[SwaggerModelProperties]]
    */
  val customSwaggerFieldSerializers = Set(
    (typeOf[URL], url)
  )

  /** Models used by Rho to create swagger definitions for complex objects
    * in the domain
    */
  object SwaggerModels {
    val sparqlQuery: Set[Model] = Set(
      ModelImpl(
        id = "SparqlQuery",
        id2 = "SparqlQuery",
        `type` = "object".some,
        description = "Sparql Query description".some,
        name = "SPARQL Query".some,
        properties = Map(
          "content" -> StringProperty(
            required = true,
            description = "Query content: textual, URL or file".some,
            enums = Set()
          ),
          "source" -> StringProperty(
            required = true,
            description =
              "Source of the query content, used to know how to access it".some,
            enums = Set("byText", "byUrl", "byFile")
          )
        ),
        example = """{
            |    "content": "https://pastebin.com/raw/4fFvFjXp",
            |    "source": "byUrl"
            |}""".stripMargin.some
      )
    )
  }

  /** Properties used by Rho to create swagger definitions for special objects
    * in the domain
    */
  object SwaggerModelProperties {
    val url: StringProperty = StringProperty(
      required = true,
      description =
        "Representation of a valid URL (including protocol, host, port... as needed)".some,
      enums = Set()
    )

  }
}
