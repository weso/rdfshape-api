package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.rdfshape.server.api.format.dataFormats.{DataFormat, Turtle}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikibase.WikibaseEntity
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.objects.wikidata.WikidataEntity
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema.WikibaseSheXerExtract.ShexerParams.{
  wikidataNamespaceQualifiers,
  wikidataProp31
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema.WikibaseSheXerExtract.{
  ShexerParams,
  mkShexerShapemap,
  shexerUri
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.Method.POST
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers.{`Content-Type` => ContentType}
import org.http4s.implicits.http4sLiteralsSyntax

import scala.util.{Failure, Success, Try}

/** Given an input [[WikibaseOperationDetails]], attempt to extract an schema (ShEx)
  * from a given entity using SheXer.
  *
  * @param operationData Data needed to perform queries against a wikibase
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note Only available for Wikidata
  * @note Should be passed a client with redirect
  * @note [[operationData]] will contain the target entity as payload and
  *       the target Wikibase as endpoint
  * @see [[https://github.com/DaniFdezAlvarez/shexer]]
  */
private[wikibase] case class WikibaseSheXerExtract(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseOperation(
      WikibaseSheXerExtract.successMessage,
      operationData,
      client
    )
    with LazyLogging {

  override lazy val targetUri: Uri = uri"" // unused

  override def performOperation: IO[WikibaseOperationResult] = {
    val entityUri = operationData.payload

    val tryResult = for {
      // Try to extract even from wikibase instances different from wikidata
      // Get the Wikibase item info from the URI submitted as payload
      wdEntity <- Try {
        val schemaUri = Uri.unsafeFromString(entityUri)
        // Build the target wikibase, as sent by the user
        if(targetWikibase == Wikidata) WikidataEntity(schemaUri)
        else new WikibaseEntity(targetWikibase, schemaUri)
      }

      // Make the SheXer params by scanning the entity sent by the client
      shexerParams = ShexerParams(
        graph = wdEntity.contentUri.renderString,
        graphSource = DataSource.URL,
        endpointGraph = targetWikibase.queryUrl,
        shapeMap = Some(mkShexerShapemap(wdEntity))
      )

      // Make the request to SheXer
      shexerRequest  = request.withEntity[Json](shexerParams.asJson)
      shexerResponse = performRequest[Json](shexerRequest)

    } yield shexerResponse

    // Pattern match errors
    tryResult match {
      case Failure(err) => IO.raiseError(new WikibaseServiceException(err))
      case Success(eitherResult) =>
        eitherResult.flatMap {
          case Left(err) => IO.raiseError(new WikibaseServiceException(err))
          case Right(shexerResponse) =>
            IO {
              WikibaseOperationResult(
                operationData = operationData,
                wikibase = targetWikibase,
                result = shexerResponse
              )
            }
        }
    }

  }

  /** Request for this operation.
    * Change method to POST, target to SheXer
    * and include the SheXer params as the entity to be processed
    */
  override def request: Request[IO] =
    super.request
      .withMethod(POST)
      .withUri(shexerUri)
      .withHeaders(Headers(ContentType(MediaType.application.`json`)))

}

private[wikibase] object WikibaseSheXerExtract {

  /** SheXer deployment address
    */
  val shexerUri: Uri         = uri"http://156.35.94.158:8081/shexer"
  private val successMessage = "Schema contents extracted successfully"

  /** Create the shapeMap parameter to be sent to SheXer given a known [[WikidataEntity]]
    *
    * @param entity  Wikidata entity being processed by SheXer
    * @param varName Name to be given to the user entity
    * @return
    */
  def mkShexerShapemap(
      entity: WikibaseEntity,
      varName: String = "userEntity"
  ): String =
    s"SPARQL 'SELECT DISTINCT ?$varName WHERE { VALUES ?$varName { wd:${entity.localName} } }'@<$varName> "

  /** Represent the JSON structure of
    * POST parameters accepted by the Shexer API.
    * Default values are provided for common uses
    *
    * @param graph                     RDF content to be analyzed
    * @param graphSource               Source where the data comes from. Use URL
    *                                  if [[graph]] is a URL pointing to raw data
    *                                  Default: [[DataSource.TEXT]]
    * @param inputFormat               RDF syntax used.
    *                                  Default: turtle
    * @param prefixes                  Tuple of namespaces-prefixes. The pairs
    *                                  provided will be used to parse the RDF
    *                                  content and write the resulting shapes.
    * @param endpointGraph             URL of an SPARQL endpoint.
    * @param instantiationProp         Property used to links an instance with its class.
    *                                  Default: "rdf:type"
    * @param namespacesToIgnore        List of namespaces whose properties should be
    *                                  ignored during the shexing process.
    *                                  Default: none
    * @param queryDepth                Indicates the depth to generate queries
    *                                  when targeting a SPARQL endpoint.
    *                                  Currently it can be 1 or 2.
    *                                  Default: 1
    * @param keepLessSpecific          It prefers to use "+" closures rather
    *                                  than exact cardinalities in the triple
    *                                  constraints
    *                                  Default: true
    * @param acceptanceThreshold       Number in [0,1] that indicates the
    *                                  minimum proportion of entities that
    *                                  should have a given feature for this
    *                                  to be accepted as a triple constraint
    *                                  in the produced shape.
    *                                  Default: 0
    * @param allClasses                If True, it generates a shape for every
    *                                  elements with at least an instance
    *                                  in the considered graph.
    *                                  Default: false
    * @param allCompliant              If False, the shapes produced may not be
    *                                  compliant with all the entities considered
    *                                  to build them. This is because it won't
    *                                  use Kleene closures for any constraint.
    *                                  Default: true
    * @param discardUselessConstraints If True, it keeps just the most possible
    *                                  specific constraint w.r.t. cardinality.
    *                                  Default: true
    * @param inferNumericTypes         If True, it tries to infer the numeric type (xsd:int, xsd:float..) of
    *                                  untyped numeric literals.
    *                                  Default, True
    * @param shapeMap                  ShapeMap to associate nodes with shapes.
    *                                  It uses the same syntax of validation shape
    *                                  maps.
    *                                  Default: None
    * @param disableComments           When set to True, the shapes do not
    *                                  include comment with ratio of entities
    *                                  compliant with a triple constraint.
    *                                  Default: false
    * @param namespaceQualifiers       When a list with elements is provided,
    *                                  the properties in the namespaces specified
    *                                  are considered to be pointers to qualifier
    *                                  nodes.
    *                                  Default: None
    * @param shapeQualifiers           If True, a shape is generated for those
    *                                  nodes detected as qualifiers according to
    *                                  Wikidata data model and the properties
    *                                  pointing to them specified in [[namespaceQualifiers]].
    *                                  Default: false
    * @see [[https://github.com/weso/shexerp3/blob/develop/ws/shexer_rest.py]]
    */
  final case class ShexerParams(
      graph: String,
      graphSource: DataSource = DataSource.TEXT,
      inputFormat: Option[DataFormat] = Some(Turtle),
      prefixes: PrefixMap = Wikidata.wikidataPrefixMap,
      endpointGraph: Uri = Wikidata.queryUrl,
      instantiationProp: Uri = wikidataProp31,
      namespacesToIgnore: List[String] = List(),
      queryDepth: Int = 1,
      acceptanceThreshold: Int = 0,
      keepLessSpecific: Boolean = true,
      allClasses: Boolean = false,
      allCompliant: Boolean = true,
      discardUselessConstraints: Boolean = true,
      inferNumericTypes: Boolean = true,
      shapeMap: Option[String],
      disableComments: Boolean = false,
      namespaceQualifiers: List[Uri] = wikidataNamespaceQualifiers,
      shapeQualifiers: Boolean = false
  )

  //noinspection HttpUrlsUsage
  object ShexerParams {

    /** Encoder to transform [[ShexerParams]] instances to JSON to be sent
      * in requests
      */
    implicit val encode: Encoder[ShexerParams] = (shexerParams: ShexerParams) =>
      {
        val graphParam = shexerParams.graphSource match {
          case DataSource.URL => "graph_url"
          case _              => "raw_graph"
        }
        val baseParams: List[(String, Json)] = List(
          (graphParam, Json.fromString(shexerParams.graph)),
          (
            "input_format",
            shexerParams.inputFormat.map(_.name.toLowerCase).asJson
          ),
          (
            "prefixes",
            Json.fromFields(
              shexerParams.prefixes.pm.map(prefixMapping =>
                (prefixMapping._1.str, Json.fromString(prefixMapping._2.str))
              )
            )
          ),
          (
            "endpoint",
            Json.fromString(shexerParams.endpointGraph.renderString)
          ),
          (
            "instantiation_prop",
            Json.fromString(shexerParams.instantiationProp.renderString)
          ),
          (
            "ignore",
            Json.fromValues(
              shexerParams.namespacesToIgnore.map(Json.fromString)
            )
          ),
          (
            "query_depth",
            Json.fromInt(shexerParams.queryDepth)
          ),
          (
            "keep_less_specific",
            Json.fromBoolean(shexerParams.keepLessSpecific)
          ),
          (
            "all_classes",
            Json.fromBoolean(shexerParams.allClasses)
          ),
          (
            "all_compliant",
            Json.fromBoolean(shexerParams.allCompliant)
          ),
          (
            "discard_useless_constraints",
            Json.fromBoolean(shexerParams.discardUselessConstraints)
          ),
          (
            "infer_untyped_nums",
            Json.fromBoolean(shexerParams.inferNumericTypes)
          ),
          (
            "disable_comments",
            Json.fromBoolean(shexerParams.disableComments)
          ),
          (
            "namespaces_for_qualifiers",
            Json.fromValues(
              shexerParams.namespaceQualifiers.map(ns =>
                Json.fromString(ns.renderString)
              )
            )
          ),
          (
            "shape_qualifiers_mode",
            Json.fromBoolean(shexerParams.shapeQualifiers)
          ),
          (
            "shape_map",
            shexerParams.shapeMap.asJson
          )
        )

        // Return final params as JSON object
        Json.fromFields(baseParams).deepDropNullValues
      }

    /** [[Uri]] representing the property "rdf:type"
      */
    private val rdfTypeProp =
      uri"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

    /** [[Uri]] representing Wikidata's "instance of" property
      *
      * @see [[https://www.wikidata.org/wiki/Property:P31]]
      */
    private val wikidataProp31: Uri =
      uri"http://www.wikidata.org/prop/direct/P31"
    private val wikidataNamespaceQualifiers = List(
      uri"http://www.wikidata.org/prop/"
    )

  }
}
