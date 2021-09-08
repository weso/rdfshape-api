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
  lazy val dataURL          = "dataURL"
  lazy val dataFile         = "dataFile"
  lazy val dataFormat       = "dataFormat"
  lazy val targetDataFormat = "targetDataFormat"

  lazy val schema             = "schema"
  lazy val schemaURL          = "schemaURL"
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

  lazy val shapeMap             = "shapeMap"
  lazy val shape_map            = "shape-map"
  lazy val shapeMapURL          = "shapeMapURL"
  lazy val shapeMapFile         = "shapeMapFile"
  lazy val shapeMapFormat       = "shapeMapFormat"
  lazy val targetShapeMapFormat = "targetShapeMapFormat"

  lazy val query     = "query"
  lazy val queryURL  = "queryURL"
  lazy val queryFile = "queryFile"

  lazy val endpoint = "endpoint"

  lazy val activeDataTab     = "activeDataTab"
  lazy val activeSchemaTab   = "activeSchemaTab"
  lazy val activeShapeMapTab = "activeShapeMapTab"
  lazy val activeQueryTab    = "activeQueryTab"

  lazy val wdEntity = "wdEntity"
  lazy val wdSchema = "wdSchema"

  lazy val url         = "url"
  lazy val urlCode     = "urlCode"
  lazy val hostname    = "hostname"
  lazy val view        = "view"
  lazy val examples    = "examples"
  lazy val manifestURL = "manifestURL"
  lazy val language    = "language"
  lazy val label       = "label"
  lazy val limit       = "limit"
  lazy val continue    = "continue"
  lazy val withDot     = "withDot"

  /** Parameter expected to contain raw RDF data (URL encoded)
    */
  object DataParameter extends OptionalQueryParamDecoderMatcher[String](data) {
    val name: String = data
  }

  /** Parameter expected to contain a compound of raw RDF data (URL encoded), formed by 2 or more RDF sources
    */
  object CompoundDataParameter
      extends OptionalQueryParamDecoderMatcher[String](compoundData) {
    val name: String = compoundData
  }

  /** Parameter expected to contain a URL where RDF data is located
    */
  object DataURLParameter
      extends OptionalQueryParamDecoderMatcher[String](dataURL) {
    val name: String = dataURL
  }

  /** Parameter expected to contain a file where RDF data is located
    */
  object DataFileParameter
      extends OptionalQueryParamDecoderMatcher[String](dataFile) {
    val name: String = dataFile
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
  object SchemaURLParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaURL) {
    val name: String = schemaURL
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

  /** Parameter expected to contain raw shapemap data (URL encoded)
    */
  object ShapeMapTextParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMap) {
    val name: String = shapeMap
  }

  /** Parameter expected to contain raw shapemap data (URL encoded)
    */
  object ShapeMapParameterAlt
      extends OptionalQueryParamDecoderMatcher[String](shape_map) {
    val name: String = shape_map
  }

  /** Parameter expected to contain a URL where a shapemap is located
    */
  object ShapeMapUrlParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapURL) {
    val name: String = shapeMapURL
  }

  /** Parameter expected to contain a file where a shapemap is located
    */
  object ShapeMapFileParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapFile) {
    val name: String = shapeMapFile
  }

  /** Parameter expected to contain a shapemap format name, referencing the user's shapemap format
    */
  object ShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapFormat) {
    val name: String = shapeMapFormat
  }

  /** Parameter expected to contain a shapemap format name, referencing the target format of a conversion
    */
  object TargetShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetShapeMapFormat) {
    val name: String = targetShapeMapFormat
  }

  /** Parameter expected to contain a raw SPARQL query (URL encoded)
    */
  object QueryParameter
      extends OptionalQueryParamDecoderMatcher[String](query) {
    val name: String = query
  }

  /** Parameter expected to contain a URL where a SPARQL query is located
    */
  object QueryURLParameter
      extends OptionalQueryParamDecoderMatcher[String](queryURL) {
    val name: String = queryURL
  }

  /** Parameter expected to contain a file where a SPARQL query is located
    */
  object QueryFileParameter
      extends OptionalQueryParamDecoderMatcher[String](queryFile) {
    val name: String = queryFile
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
  object ActiveDataTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeDataTab) {
    val name: String = activeDataTab
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in schema-related operations
    */
  object ActiveSchemaTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeSchemaTab) {
    val name: String = activeSchemaTab
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in shapemap-related operations
    */
  object ActiveShapeMapTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeShapeMapTab) {
    val name: String = activeShapeMapTab
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in query-related operations
    */
  object ActiveQueryTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeQueryTab) {
    val name: String = activeQueryTab
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
