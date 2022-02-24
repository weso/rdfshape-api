package es.weso.rdfshape.server.api

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import es.weso.rdfshape.server.api.format.dataFormats.ShapeMapFormat
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.{
  SparqlQuery,
  SparqlQuerySource
}
import es.weso.rdfshape.server.api.routes.shapemap.logic.{
  ShapeMap,
  ShapeMapSource
}
import es.weso.rdfshape.server.api.swagger.SwaggerModelProperties.url
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  ContentParameter,
  FormatParameter,
  SourceParameter
}
import org.http4s.rho.RhoMiddleware
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.{
  DefaultSwaggerFormats,
  SwaggerFormats,
  SwaggerMetadata
}

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
      swaggerFormats = allSwaggerFormats,
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

  /** Merge of both object and field serializer sets as SwaggerFormats
    */
  lazy val allSwaggerFormats: SwaggerFormats = {
    // Add all object models
    val objectSerializers = customSwaggerSerializers.foldLeft(
      DefaultSwaggerFormats
    )((prev, curr) =>
      curr match {
        case (classType, serializerModel) =>
          prev.withSerializers(classType, serializerModel)
      }
    )
    // Add all property models on top
    customSwaggerFieldSerializers.foldLeft(
      objectSerializers
    )((prev, curr) =>
      curr match {
        case (classType, prop) =>
          prev.withFieldSerializers(classType, prop)
      }
    )
  }

  /** Set of all swagger object serializers defined in [[SwaggerModels]]
    */
  val customSwaggerSerializers = Set(
    (typeOf[ShapeMap], shapeMap),
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
    val shapeMap: Set[Model] = Set(
      ModelImpl(
        id = classOf[ShapeMap].getSimpleName,
        id2 = classOf[ShapeMap].getSimpleName,
        `type` = "object".some,
        description = "ShapeMap matching nodes with shapes".some,
        name = "ShapeMap".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            description = "ShapeMap contents".some,
            enums = Set(),
            pattern = "(<Node>@<Shape>)+".some
          ),
          FormatParameter.name -> StringProperty(
            required = true,
            description = "ShapeMap format".some,
            enums = ShapeMapFormat.availableFormats.map(_.name).toSet
          ),
          SourceParameter.name -> StringProperty(
            required = true,
            description =
              "Source of the shapeMap content, used to know how to access it".some,
            enums = SparqlQuerySource.values
          )
        ),
        externalDocs = ExternalDocs(
          "W3C Draft - ShapeMap Structure and Language",
          "https://shex.io/shape-map/"
        ).some,
        example = s"""{
             |    "content": "<http://example.org/Alice>@<http://example.org/User>",
             |    "source": "${ShapeMapSource.TEXT}"
             |}""".stripMargin.some
      )
    )

    val sparqlQuery: Set[Model] = Set(
      ModelImpl(
        id = classOf[SparqlQuery].getSimpleName,
        id2 = classOf[SparqlQuery].getSimpleName,
        `type` = "object".some,
        description = "SPARQL query used to query RDF data".some,
        name = "SPARQL Query".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            description = "Query contents".some,
            enums = Set()
          ),
          SourceParameter.name -> StringProperty(
            required = true,
            description =
              "Source of the query content, used to know how to access it".some,
            enums = SparqlQuerySource.values
          )
        ),
        externalDocs = ExternalDocs(
          "W3C - SPARQL query language for RDF",
          "https://www.w3.org/TR/rdf-sparql-query/"
        ).some,
        example = s"""{
             |    "content": "SELECT ?a ?b ?c WHERE { ?a ?b ?c } LIMIT 1",
             |    "source": "${SparqlQuerySource.TEXT}"
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
      enums = Set(),
      default = "https://...".some
    )

  }
}
