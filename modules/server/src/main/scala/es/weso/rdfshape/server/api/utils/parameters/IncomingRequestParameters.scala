package es.weso.rdfshape.server.api.utils.parameters

import org.http4s.dsl.io.{
  OptionalQueryParamDecoderMatcher,
  QueryParamDecoderMatcher
}

/** Definitions for all the possible parameters that may come from client requests
  */
object IncomingRequestParameters {

  // String constants representing each parameter name expected by the server
  lazy val data             = "data"
  lazy val compoundData     = "compoundData"
  lazy val dataFormat       = "dataFormat"
  lazy val targetDataFormat = "targetDataFormat"

  lazy val schema             = "schema"
  lazy val schemaUrl          = "schemaUrl"
  lazy val schemaFile         = "schemaFile"
  lazy val schemaFormat       = "schemaFormat"
  lazy val schemaEngine       = "schemaEngine"
  lazy val targetSchemaFormat = "targetSchemaFormat"
  lazy val targetSchemaEngine = "targetSchemaEngine"
  lazy val inference          = "inference"
  lazy val triggerMode        = "triggerMode"

  lazy val shape        = "shape"
  lazy val entity       = "entity"
  lazy val node         = "node"
  lazy val nodeSelector = "nodeSelector"

  lazy val shapemap             = "shapemap"
  lazy val shape_map            = "shape-map"
  lazy val shapemapFormat       = "shapemapFormat"
  lazy val targetShapemapFormat = "targetShapemapFormat"

  lazy val query = "query"

  lazy val endpoint = "endpoint"

  lazy val dataSource     = "dataSource"
  lazy val schemaSource   = "schemaSource"
  lazy val shapemapSource = "shapemapSource"
  lazy val querySource    = "querySource"

  lazy val wdEntity = "wdEntity"
  lazy val wdSchema = "wdSchema"

  lazy val url         = "url"
  lazy val urlCode     = "urlCode"
  lazy val hostname    = "hostname"
  lazy val view        = "view"
  lazy val examples    = "examples"
  lazy val manifestUrl = "manifestUrl"
  lazy val language    = "language"
  lazy val label       = "label"
  lazy val limit       = "limit"
  lazy val continue    = "continue"
  lazy val withDot     = "withDot"

  /** Parameter expected to contain RDF data contents (URL encoded)
    *
    * @note These contents may be raw data, a URL with the data or a File with the data.
    *       The source of the data is therefore specified by [[DataSourceParameter]]
    */
  object DataParameter extends OptionalQueryParamDecoderMatcher[String](data) {
    val name: String = data
  }

  /** Parameter expected to contain a compound of RDF data (URL encoded), formed by 2 or more RDF sources
    */
  object CompoundDataParameter
      extends OptionalQueryParamDecoderMatcher[String](compoundData) {
    val name: String = compoundData
  }

  /** Parameter expected to contain an RDF format name, referencing the user's data format
    */
  object DataFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](dataFormat) {
    val name: String = dataFormat
  }

  /** Parameter expected to contain an RDF format name, referencing the target format of a conversion
    */
  object TargetDataFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetDataFormat) {
    val name: String = targetDataFormat
  }

  /** Parameter expected to contain raw schema data (URL encoded)
    */
  object SchemaParameter
      extends OptionalQueryParamDecoderMatcher[String](schema) {
    val name: String = schema
  }

  /** Parameter expected to contain a URL where a validation schema is located
    */
  object SchemaUrlParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaUrl) {
    val name: String = schemaUrl
  }

  /** Parameter expected to contain the contents a file where a validation schema is located
    */
  object SchemaFileParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaFile) {
    val name: String = schemaFile
  }

  /** Parameter expected to contain an schema format name, referencing the user's schema format
    */
  object SchemaFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaFormat) {
    val name: String = schemaFormat
  }

  /** Parameter expected to contain an schema engine name, referencing the user's desired schema engine
    */
  object SchemaEngineParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaEngine) {
    val name: String = schemaEngine
  }

  /** Parameter expected to contain an schema format name, referencing the target format of a conversion
    */
  object TargetSchemaFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetSchemaFormat) {
    val name: String = targetSchemaFormat
  }

  /** Parameter expected to contain an schema engine name, referencing the target engine of a conversion
    */
  object TargetSchemaEngineParameter
      extends OptionalQueryParamDecoderMatcher[String](targetSchemaEngine) {
    val name: String = targetSchemaEngine
  }

  /** Parameter expected to contain the inference applied in data validations
    */
  object InferenceParameter
      extends OptionalQueryParamDecoderMatcher[String](inference) {
    val name: String = inference
  }

  /** Parameter expected to contain the trigger mode present applied in data validations
    */
  object TriggerModeParameter
      extends OptionalQueryParamDecoderMatcher[String](triggerMode) {
    val name: String = triggerMode
  }

  /** Parameter expected to contain a shape name or identifier on wikibase operations
    */
  object ShapeParameter
      extends OptionalQueryParamDecoderMatcher[String](shape) {
    val name: String = shape
  }

  /** Parameter expected to contain an entity name or identifier on wikibase operations
    */
  object EntityParameter
      extends OptionalQueryParamDecoderMatcher[String](entity) {
    val name: String = entity
  }

  /** Parameter expected to contain a node name or identifier on SPARQL query operations
    */
  object NodeParameter extends OptionalQueryParamDecoderMatcher[String](node) {
    val name: String = node
  }

  /** Parameter expected to contain a node name or identifier on schema-extraction operations
    */
  object NodeSelectorParameter
      extends OptionalQueryParamDecoderMatcher[String](nodeSelector) {
    val name: String = nodeSelector
  }

  /** Parameter expected to contain Shapemap contents (URL encoded)
    *
    * @note These contents may be raw data, a URL with the data or a File with the data.
    *       The source of the data is therefore specified by [[ShapemapSourceParameter]]
    */
  object ShapeMapParameter
      extends OptionalQueryParamDecoderMatcher[String](shapemap) {
    val name: String = shapemap
  }

  /** Alternative parameter with the same uses as [[ShapeMapParameter]]
    */
  object ShapeMapParameterAlt
      extends OptionalQueryParamDecoderMatcher[String](shape_map) {
    val name: String = shape_map
  }

  /** Parameter expected to contain a shapemap format name, referencing the user's shapemap format
    */
  object ShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](shapemapFormat) {
    val name: String = shapemapFormat
  }

  /** Parameter expected to contain a shapemap format name, referencing the target format of a conversion
    */
  object TargetShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetShapemapFormat) {
    val name: String = targetShapemapFormat
  }

  /** Parameter expected to contain SPARQL query data contents (URL encoded)
    *
    * @note These contents may be raw data, a URL with the query or a File with the query.
    *       The source of the query is therefore specified by [[QuerySourceParameter]]
    */
  object QueryParameter
      extends OptionalQueryParamDecoderMatcher[String](query) {
    val name: String = query
  }

  /** Parameter expected to contain a raw endpoint location
    */
  object EndpointParameter
      extends OptionalQueryParamDecoderMatcher[String](endpoint) {
    val name: String = endpoint
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in data-related operations
    */
  object DataSourceParameter
      extends OptionalQueryParamDecoderMatcher[String](dataSource) {
    val name: String = dataSource
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in schema-related operations
    */
  object SchemaSourceParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaSource) {
    val name: String = schemaSource
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in shapemap-related operations
    */
  object ShapemapSourceParameter
      extends OptionalQueryParamDecoderMatcher[String](shapemapSource) {
    val name: String = shapemapSource
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in query-related operations
    */
  object QuerySourceParameter
      extends OptionalQueryParamDecoderMatcher[String](querySource) {
    val name: String = querySource
  }

  /** Parameter expected to contain a valid identifier/name/label of a wikidata entity
    * in wikidata-related operations
    */
  object WdEntityParameter extends QueryParamDecoderMatcher[String](wdEntity) {
    val name: String = wdEntity
  }

  /** Parameter expected to contain a valid identifier/name/label of a wikidata schema
    * in wikidata-related operations
    */
  object WdSchemaParameter extends QueryParamDecoderMatcher[String](wdSchema) {
    val name: String = wdSchema
  }

  /** Parameter expected to contain a valid language code, normally for
    * wikidata-related operations that return data in a user-selected language
    *
    * @note See {@linkplain https:// en.wikipedia.org / wiki / List_of_ISO_639 - 1 _codes}
    */
  object LanguageParameter extends QueryParamDecoderMatcher[String](language) {
    val name: String = language
  }

  /** Parameter expected to contain a valid identifier/name/label of a wikibase entity
    * in wikibase-related operations
    */
  object LabelParameter extends QueryParamDecoderMatcher[String](label) {
    val name: String = label
  }

  /** Parameter expected to contain a valid URL
    * Used for multiple operations (permalinks, fetching information...)
    */
  object UrlParameter extends QueryParamDecoderMatcher[String](url) {
    val name: String = url
  }

  /** Parameter expected to contain a permalink identifier for the permalink service to fetch its corresponding link
    */
  object UrlCodeParameter extends QueryParamDecoderMatcher[String](urlCode) {
    val name: String = urlCode
  }

  /** Parameter expected to contain a positive numeric value to serve as a limit of a query/search operation, normally
    * in wikibase-related operations
    */
  object LimitParameter
      extends OptionalQueryParamDecoderMatcher[String](limit) {
    val name: String = limit
  }

  /** Parameter expected to contain a positive numeric value to serve as the offset where to continue a search operation, normally
    * in wikibase-related operations
    */
  object ContinueParameter
      extends OptionalQueryParamDecoderMatcher[String](continue) {
    val name: String = continue
  }

}
