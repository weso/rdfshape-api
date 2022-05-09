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
  lazy val dataCompound     = "dataCompound"
  lazy val dataFormat       = "dataFormat"
  lazy val dataTargetFormat = "dataTargetFormat"

  lazy val schema             = "schema"
  lazy val schemaFormat       = "schemaFormat"
  lazy val schemaEngine       = "schemaEngine"
  lazy val targetSchemaFormat = "schemaTargetFormat"
  lazy val targetSchemaEngine = "schemaTargetEngine"
  lazy val inference          = "inference"
  lazy val triggerMode        = "triggerMode"

  lazy val shape        = "shape"
  lazy val entity       = "entity"
  lazy val node         = "node"
  lazy val nodeSelector = "nodeSelector"

  lazy val shapeMap       = "shape-map"
  lazy val shapeMapFormat = "shapeMapFormat"

  lazy val query = "query"

  lazy val endpoint = "endpoint"

  lazy val content = "content"

  lazy val message = "message"

  lazy val reason = "reason"

  lazy val format = "format"

  lazy val engine = "engine"

  lazy val `type` = "type"

  lazy val targetFormat = "targetFormat"
  lazy val targetEngine = "targetEngine"

  lazy val source         = "source"
  lazy val dataSource     = "dataSource"
  lazy val schemaSource   = "schemaSource"
  lazy val shapeMapSource = "shapeMapSource"
  lazy val querySource    = "querySource"

  lazy val configuration = "configuration"

  lazy val validator       = "validator"
  lazy val haltOnInvalid   = "haltOnInvalid"
  lazy val haltOnErrored   = "haltOnErrored"
  lazy val concurrentItems = "concurrentItems"

  lazy val extractor = "extractor"
  lazy val timeout   = "timeout"

  lazy val stream  = "stream"
  lazy val server  = "server"
  lazy val port    = "port"
  lazy val topic   = "topic"
  lazy val groupId = "groupId"

  lazy val wdEntity = "wdEntity"
  lazy val wdSchema = "wdSchema"

  lazy val payload     = "payload"
  lazy val url         = "url"
  lazy val urlCode     = "urlCode"
  lazy val hostname    = "hostname"
  lazy val view        = "view"
  lazy val examples    = "examples"
  lazy val manifestUrl = "manifestUrl"
  lazy val language    = "language"
  lazy val languages   = "languages"
  lazy val label       = "label"
  lazy val limit       = "limit"
  lazy val wbFormat    = "wbFormat"
  lazy val continue    = "continue"
  lazy val withDot     = "withDot"

  /** Parameter expected to contain the content inputted by the user for a certain
    * operation
    *
    * @note These contents may be raw data, a URL/File with the contents...
    *       The source of the query is therefore specified in other [[SourceParameter]]
    */
  object ContentParameter
      extends OptionalQueryParamDecoderMatcher[String](content) {
    val name: String = content
  }

  /** Parameter expected to contain a generic message
    */
  object MessageParameter
      extends OptionalQueryParamDecoderMatcher[String](message) {
    val name: String = message
  }

  /** Parameter expected to contain the reason or an explanation of something
    */
  object ReasonParameter
      extends OptionalQueryParamDecoderMatcher[String](reason) {
    val name: String = reason
  }

  /** Parameter expected to contain the format of the content inputted by the user for a certain
    * operation
    */
  object FormatParameter
      extends OptionalQueryParamDecoderMatcher[String](format) {
    val name: String = format
  }

  /** Parameter expected to contain the target data format for a certain
    * conversion operation
    */
  object TargetFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetFormat) {
    val name: String = targetFormat
  }

  /** Parameter expected to contain the target schema engine for a certain
    * conversion operation
    */
  object TargetEngineParameter
      extends OptionalQueryParamDecoderMatcher[String](targetEngine) {
    val name: String = targetEngine
  }

  /** Parameter expected to contain the engine of the schema inputted by the user for a certain
    * operation
    */
  object EngineParameter
      extends OptionalQueryParamDecoderMatcher[String](engine) {
    val name: String = engine
  }

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
      extends OptionalQueryParamDecoderMatcher[String](dataCompound) {
    val name: String = dataCompound
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
      extends OptionalQueryParamDecoderMatcher[String](dataTargetFormat) {
    val name: String = dataTargetFormat
  }

  /** Parameter expected to contain schema contents (URL encoded)
    *
    * @note These contents may be raw data, a URL with the schema or a File with the schema.
    *       The source of the schema is therefore specified by [[SchemaSourceParameter]]
    */
  object SchemaParameter
      extends OptionalQueryParamDecoderMatcher[String](schema) {
    val name: String = schema
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
      extends OptionalQueryParamDecoderMatcher[String](shapeMap) {
    val name: String = shapeMap
  }

  /** Parameter expected to contain a shapemap format name, referencing the user's shapemap format
    */
  object ShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapFormat) {
    val name: String = shapeMapFormat
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

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client
    * for any operation
    */
  object SourceParameter
      extends OptionalQueryParamDecoderMatcher[String](source) {
    val name: String = source
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
      extends OptionalQueryParamDecoderMatcher[String](shapeMapSource) {
    val name: String = shapeMapSource
  }

  /** Parameter expected to contain a valid identifier of the source of the data sent by the client (currently raw data, a URL or a file)
    * in query-related operations
    */
  object QuerySourceParameter
      extends OptionalQueryParamDecoderMatcher[String](querySource) {
    val name: String = querySource
  }

  /** Parameter expected to contain a payload for later use querying wikibase's API
    */
  object PayloadParameter extends QueryParamDecoderMatcher[String](payload) {
    val name: String = payload
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
    * wikidata-related operations that search for data in a user-selected language
    *
    * @note See [[https:// en.wikipedia.org / wiki / List_of_ISO_639 - 1 _codes]]
    */
  object LanguageParameter extends QueryParamDecoderMatcher[String](language) {
    val name: String = language
  }

  /** Parameter expected to contain a list of language codes, normally for
    * wikidata-related operations that return data in a user-selected language
    *
    * @note See [[https:// en.wikipedia.org / wiki / List_of_ISO_639 - 1 _codes]]
    */
  object LanguagesParameter
      extends QueryParamDecoderMatcher[String](languages) {
    val name: String = languages
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

  /** Flexible parameter expected to contain the type of a given item in a given operation
    */
  object TypeParameter
      extends OptionalQueryParamDecoderMatcher[String](`type`) {
    val name: String = `type`
  }

  /** Parameter expected to contain a positive numeric value to serve as the offset where to continue a search operation, normally
    * in wikibase-related operations
    */
  object ContinueParameter
      extends OptionalQueryParamDecoderMatcher[String](continue) {
    val name: String = continue
  }

  /** Parameter expected to contain a nested configuration object
    */
  object ConfigurationParameter
      extends OptionalQueryParamDecoderMatcher[String](configuration) {
    val name: String = configuration
  }

  /** Parameter expected to contain a validator information in stream validations
    */
  object ValidatorParameter
      extends OptionalQueryParamDecoderMatcher[String](validator) {
    val name: String = validator
  }

  /** Parameter expected to contain whether if stream validations should halt
    * when an item turns out invalid
    */
  object HaltOnInvalidParameter
      extends OptionalQueryParamDecoderMatcher[Boolean](haltOnInvalid) {
    val name: String = haltOnInvalid
  }

  /** Parameter expected to contain whether if stream validations should halt
    * when an error occurs during item validation
    */
  object HaltOnErroredParameter
      extends OptionalQueryParamDecoderMatcher[Boolean](haltOnErrored) {
    val name: String = haltOnErrored
  }

  /** Parameter expected to contain how many items should be processed in parallel
    * in stream validations
    */
  object ConcurrentItemsParameter
      extends OptionalQueryParamDecoderMatcher[Int](concurrentItems) {
    val name: String = concurrentItems
  }

  /** Parameter expected to contain a extractor information in stream validations
    */
  object ExtractorParameter
      extends OptionalQueryParamDecoderMatcher[String](extractor) {
    val name: String = extractor
  }

  /** Parameter expected to contain a numeric amount of time expressing a timeout
    */
  object TimeoutParameter
      extends OptionalQueryParamDecoderMatcher[String](extractor) {
    val name: String = extractor
  }

  /** Parameter expected to contain the input stream information in stream validations
    */
  object StreamParameter
      extends OptionalQueryParamDecoderMatcher[String](stream) {
    val name: String = stream
  }

  /** Parameter expected to contain a server address/hostname
    */
  object ServerParameter
      extends OptionalQueryParamDecoderMatcher[String](server) {
    val name: String = server
  }

  /** Parameter expected to contain a server port
    */
  object PortParameter extends OptionalQueryParamDecoderMatcher[Int](port) {
    val name: String = port
  }

  /** Parameter expected to contain a certain topic
    *
    * In stream validations, defines the topic in which data is expected to come
    * from
    */
  object TopicParameter extends OptionalQueryParamDecoderMatcher[Int](topic) {
    val name: String = topic
  }

  /** Parameter expected to contain a certain group identifier
    *
    * In stream validations, defines the group in which data is expected to be
    * consumed
    */
  object GroupIdParameter
      extends OptionalQueryParamDecoderMatcher[Int](groupId) {
    val name: String = groupId
  }

}
