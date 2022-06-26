package es.weso.rdfshape.server.api

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import es.weso.rdfshape.server.api.definitions.ApiDefinitions.{api, wikidataUrl}
import es.weso.rdfshape.server.api.definitions.{ApiDefaults, ApiDefinitions}
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  RdfFormat,
  ShapeMapFormat
}
import es.weso.rdfshape.server.api.format.{Format, FormatCompanion}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.api.routes.endpoint.logic.query.{
  SparqlQuery,
  SparqlQuerySource
}
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.{
  TriggerMode,
  TriggerModeType
}
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.shapemap.logic.{
  ShapeMap,
  ShapeMapSource
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperationDetails,
  WikibaseOperationFormats
}
import es.weso.rdfshape.server.api.swagger.SwaggerModelProperties._
import es.weso.rdfshape.server.api.swagger.SwaggerModels._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters._
import es.weso.schema.{Schema => SchemaW}
import org.http4s.rho.RhoMiddleware
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.{io => ioSwagger}
import org.http4s.rho.swagger.{
  DefaultSwaggerFormats,
  SwaggerFormats,
  SwaggerMetadata
}

import java.net.URL
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.typeOf

/** @note For the streaming validations endpoint, remember to add some responses
  *       information manually into the generated swagger-file, e.g.:
  *       responses:
  *        '200':
  *          description: Dummy response, a WebSockets connection should start
  *        '400':
  *          description: A non-WebSockets request was received
  *        '500':
  *          description: Handshake failed
  */
package object swagger {
  // Import all models

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
      swaggerMetadata = baseSwaggerMetadata
    )

  /** Merge of both object and field serializer sets as SwaggerFormats
    */
  private lazy val allSwaggerFormats: SwaggerFormats = {
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

  /** Metadata object with the API properties to be displayed in Swagger
    */
  private val baseSwaggerMetadata: SwaggerMetadata = SwaggerMetadata(
    apiInfo = Info(
      title = "RDFShape API",
      version =
        buildinfo.BuildInfo.version, // programmatically get project version
      description = s"""
           | RDFShape is web API for semantic data analysis and validation
           | implemented in Scala using http4s (https://http4s.org/) and 
           | documented with Rho (https://github.com/http4s/rho).
           |""".stripMargin.some,
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
    basePath = s"/$api".some, // All routes are prefixed by "/api"
    schemes = List(Scheme.HTTP, Scheme.HTTPS),
    consumes = List("application/json"),
    produces = List("text/plain; charset=utf-8", "application/json")
  )

  /** Set of all swagger object serializers defined in [[SwaggerModels]]
    */
  private val customSwaggerSerializers: Set[(universe.Type, Set[Model])] =
    Set(
      (typeOf[Data], dataModel),
      (typeOf[Schema], schemaModel),
      (typeOf[ShapeMap], shapeMapModel),
      (typeOf[TriggerMode], triggerModeModel),
      (typeOf[WikibaseOperationDetails], wikibaseOperationDetailsModel),
      (typeOf[SparqlQuery], sparqlQueryModel),
      (typeOf[URL], urlModel)
    )

  /** Set of all swagger field serializers defined in [[SwaggerModelProperties]]
    */
  private val customSwaggerFieldSerializers = Set(
    (typeOf[Data], dataProperty),
    (typeOf[Schema], schemaProperty),
    (typeOf[ShapeMap], shapeMapProperty),
    (typeOf[TriggerMode], triggerModeProperty),
    (typeOf[SparqlQuery], sparqlQueryProperty),
    (typeOf[WikibaseOperationDetails], wikibaseOperationDetailsProperty),
    (typeOf[URL], urlProperty),
    (typeOf[DataFormat], dataFormatProperty),
    (typeOf[RdfFormat], rdfFormatProperty),
    (typeOf[SchemaFormat], schemaFormatProperty),
    (typeOf[ShapeMapFormat], shapeMapFormatProperty),
    (typeOf[SchemaW], schemaEngineProperty)
  )

  /** Models used by Rho to create swagger definitions for complex objects
    * in the domain
    */
  //noinspection HttpUrlsUsage
  object SwaggerModels {

    val urlModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[URL].getSimpleName,
        id2 = classOf[URL].getSimpleName,
        `type` = "string".some,
        name = "URL".some,
        description =
          "Representation of a valid URL (including protocol, host, port... as needed)".some,
        isSimple = true,
        example = wikidataUrl.some,
        externalDocs = ExternalDocs(
          "RFC 1738 - Uniform Resource Locators (URL)",
          "https://datatracker.ietf.org/doc/html/rfc1738"
        ).some
      )
    )
    val dataModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[Data].getSimpleName,
        id2 = classOf[Data].getSimpleName,
        `type` = "object".some,
        description = "RDF data instance to be processed by the API".some,
        name = "Data".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            title = "Contents".some,
            description = "RDF data contents, as inputted by the user".some,
            enums = Set(),
            default = Defaults.rdfDataContent.some
          ),
          FormatParameter.name -> rdfFormatProperty,
          InferenceParameter.name -> StringProperty(
            required = false,
            title = "Inference".some,
            description = "Inference to be applied on the data".some,
            enums = ApiDefinitions.availableInferenceEngines.map(_.name).toSet,
            default = ApiDefaults.defaultInferenceEngine.name.some
          ),
          SourceParameter.name -> StringProperty(
            required = true,
            title = "Source".some,
            description =
              "Source of the RDF data, used to know how to access it".some,
            enums = DataSource.values,
            default = DataSource.default.some
          )
        ),
        externalDocs = ExternalDocs(
          "W3C - Resource Description Framework (RDF)",
          "https://www.w3.org/RDF/"
        ).some
      )
    )
    val schemaModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[Schema].getSimpleName,
        id2 = classOf[Schema].getSimpleName,
        `type` = "object".some,
        description =
          "Schema instance to be processed by the API (Shape Expressions or SHACL)".some,
        name = "Schema".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            title = "Contents".some,
            description = "Schema contents, as inputted by the user".some,
            enums = Set(),
            default = Defaults.schemaContent.some
          ),
          FormatParameter.name -> schemaFormatProperty,
          EngineParameter.name -> schemaEngineProperty,
          SourceParameter.name -> StringProperty(
            required = true,
            title = "Source".some,
            description =
              "Source of the schema, used to know how to access it".some,
            enums = SchemaSource.values,
            default = SchemaSource.default.some
          )
        ),
        externalDocs = ExternalDocs(
          "Validating RDF Data - Comparing ShEx and SHACL",
          "https://book.validatingrdf.com/bookHtml013.html"
        ).some
      )
    )
    val triggerModeModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[TriggerMode].getSimpleName,
        id2 = classOf[TriggerMode].getSimpleName,
        `type` = "object".some,
        description =
          "Trigger mode used in data validations, might contain a shapeMap for ShEx validations".some,
        name = "ShapeMap".some,
        properties = Map(
          TypeParameter.name -> StringProperty(
            required = true,
            title = "Type".some,
            description =
              "Type of the trigger, used to know how to process the validation".some,
            enums = TriggerModeType.values,
            default = TriggerModeType.default.some
          ),
          DataParameter.name -> RefProperty(
            required = false,
            title = "Data".some,
            description =
              "The data being processed in the validation where this trigger mode appears. If the data was already sent in the request, it is not necessary to embed it here".some,
            ref = classOf[Data].getSimpleName
          ),
          SchemaParameter.name -> RefProperty(
            required = false,
            title = "Schema".some,
            description =
              "The schema being used in the validation where this trigger mode appears. If the schema was already sent in the request, it is not necessary to embed it here".some,
            ref = classOf[Schema].getSimpleName
          ),
          ShapeMapParameter.name -> RefProperty(
            required = false,
            title = "ShapeMap".some,
            description =
              "The schema being used in the validation where this trigger mode appears. Only required in ShEx validations".some,
            ref = classOf[ShapeMap].getSimpleName
          )
        )
      )
    )
    val shapeMapModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[ShapeMap].getSimpleName,
        id2 = classOf[ShapeMap].getSimpleName,
        `type` = "object".some,
        description = "ShapeMap matching nodes with shapes".some,
        name = "ShapeMap".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            title = "Contents".some,
            description = "ShapeMap contents, as inputted by the user".some,
            enums = Set(),
            pattern = "(<Node>@<Shape>)+".some,
            default = Defaults.shapeMapContent.some
          ),
          FormatParameter.name -> shapeMapFormatProperty,
          SourceParameter.name -> StringProperty(
            required = true,
            title = "Source".some,
            description =
              "Source of the shapeMap content, used to know how to access it".some,
            enums = ShapeMapSource.values,
            default = ShapeMapSource.default.some
          )
        ),
        externalDocs = ExternalDocs(
          "W3C Draft - ShapeMap Structure and Language",
          "https://shex.io/shape-map/"
        ).some
      )
    )
    val sparqlQueryModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[SparqlQuery].getSimpleName,
        id2 = classOf[SparqlQuery].getSimpleName,
        `type` = "object".some,
        description = "SPARQL query used to query RDF data".some,
        name = "SPARQL Query".some,
        properties = Map(
          ContentParameter.name -> StringProperty(
            required = true,
            title = "Contents".some,
            description = "Query contents, as inputted by the user".some,
            enums = Set(),
            default = Defaults.sparqlQueryContent.some
          ),
          SourceParameter.name -> StringProperty(
            required = true,
            title = "Source".some,
            description =
              "Source of the query contents, used to know how to access it".some,
            enums = SparqlQuerySource.values,
            default = SparqlQuerySource.default.some
          )
        ),
        externalDocs = ExternalDocs(
          "W3C - SPARQL query language for RDF",
          "https://www.w3.org/TR/rdf-sparql-query/"
        ).some
      )
    )
    val wikibaseOperationDetailsModel: Set[Model] = Set(
      ModelImpl(
        id = classOf[WikibaseOperationDetails].getSimpleName,
        id2 = classOf[WikibaseOperationDetails].getSimpleName,
        `type` = "object".some,
        description =
          "Set of parameters required to perform operations on a Wikibase instance".some,
        name = "Wikibase Operation details".some,
        properties = Map(
          EndpointParameter.name -> RefProperty(
            ref = classOf[URL].getSimpleName,
            required = false,
            title = "Endpoint".some,
            description =
              "URL pointing to the wikibase instance root or SPARQL query endpoint. Defaults to Wikidata.".some
          ),
          PayloadParameter.name -> StringProperty(
            required = true,
            title = "Payload".some,
            description = "Data accompanying the request to the wikibase".some,
            enums = Set()
          ),
          FormatParameter.name -> StringProperty(
            required = false,
            title = "Format".some,
            description = "Format in which results are requested".some,
            enums = WikibaseOperationFormats.values
          ),
          LimitParameter.name -> StringProperty(
            required = false,
            title = "Limit".some,
            description =
              "Maximum amount of results queried in search operations".some,
            enums = Set(),
            pattern = "Number >= 0".some
          ),
          ContinueParameter.name -> StringProperty(
            required = false,
            title = "Continue".some,
            description = "Offset where to continue a search operation".some,
            enums = Set(),
            pattern = "Number >= 0".some
          ),
          LanguageParameter.name -> StringProperty(
            required = false,
            title = "Search language".some,
            description =
              "Language to be used in a wikibase search operation, as well as the language for the returned results".some,
            enums = Set(),
            default = "en".some
          ),
          LanguagesParameter.name -> ArrayProperty(
            required = false,
            title = "Result language".some,
            description =
              "Filter the languages returned in GET queries with internationalized results (empty list returns all available languages)".some,
            items = StringProperty(
              enums = Set(),
              default = "en".some
            )
          )
        )
      )
    )

    /** Default values used in custom Swagger property definitions
      */
    //noinspection HttpUrlsUsage
    private[api] object Defaults {
      val rdfDataContent: String =
        "@prefix : <http://example.org/> . :alice a :item ."
      val schemaContent =
        "PREFIX : <http://example.org/> PREFIX schema: <http://schema.org/>  PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#> :User { schema:name xsd:string }"
      val sparqlQueryContent = "SELECT ?a ?b ?c WHERE { ?a ?b ?c } LIMIT 1"
      val shapeMapContent =
        "<http://example.org/Alice>@<http://example.org/User>"
    }
  }

  /** Properties used by Rho to create swagger definitions for special objects
    * in the domain
    */
  //noinspection HttpUrlsUsage
  object SwaggerModelProperties {

    // Force properties to reference the correct model defined above
    val urlProperty: RefProperty    = RefProperty(classOf[URL].getSimpleName)
    val dataProperty: RefProperty   = RefProperty(classOf[Data].getSimpleName)
    val schemaProperty: RefProperty = RefProperty(classOf[Schema].getSimpleName)
    val shapeMapProperty: RefProperty = RefProperty(
      classOf[ShapeMap].getSimpleName
    )
    val triggerModeProperty: RefProperty = RefProperty(
      classOf[TriggerMode].getSimpleName
    )
    val sparqlQueryProperty: RefProperty = RefProperty(
      classOf[SparqlQuery].getSimpleName
    )
    val wikibaseOperationDetailsProperty: RefProperty = RefProperty(
      classOf[WikibaseOperationDetails].getSimpleName
    )

    val rdfFormatProperty: StringProperty    = mkFormatProperty(RdfFormat)
    val dataFormatProperty: StringProperty   = mkFormatProperty(DataFormat)
    val schemaFormatProperty: StringProperty = mkFormatProperty(SchemaFormat)
    val shapeMapFormatProperty: StringProperty = mkFormatProperty(
      ShapeMapFormat
    )

    val schemaEngineProperty: StringProperty = StringProperty(
      required = true,
      title = "Engine".some,
      description =
        "Engine in which the schema is redacted (ShEx or any other SHACL engine)".some,
      enums = ApiDefinitions.availableSchemaEngines.map(_.name).toSet,
      default = ApiDefaults.defaultSchemaEngine.name.some
    )

    /** Automated algorithm for creating SwaggerProperty definitions for any
      * [[Format]] sub-type
      *
      * @param companion Companion object of the Format
      * @tparam F Format sub-type
      * @return A [[StringProperty]] defining the format in Swagger
      */
    private def mkFormatProperty[F <: Format](
        companion: FormatCompanion[F]
    ) = {
      val propertyName = companion.getClass.getSimpleName.stripSuffix("$")

      StringProperty(
        required = true,
        title = propertyName.some,
        description = s"$propertyName in which the input is redacted".some,
        enums = companion.availableFormats.map(_.name).toSet,
        default = companion.default.name.some
      )
    }

  }

}
