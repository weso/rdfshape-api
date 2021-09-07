package es.weso.rdfshape.server.api.utils.parameters

import org.http4s.dsl.io.{
  OptionalQueryParamDecoderMatcher,
  QueryParamDecoderMatcher
}

/** Definitions for all the possible parameters that may come from client requests
  */
object IncomingRequestParameters {
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

  object DataParameter extends OptionalQueryParamDecoderMatcher[String](data) {
    val name: String = data
  }

  object CompoundDataParameter
      extends OptionalQueryParamDecoderMatcher[String](compoundData) {
    val name: String = compoundData
  }

  object DataURLParameter
      extends OptionalQueryParamDecoderMatcher[String](dataURL) {
    val name: String = dataURL
  }

  object DataFileParameter
      extends OptionalQueryParamDecoderMatcher[String](dataFile) {
    val name: String = dataFile
  }

  object DataFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](dataFormat) {
    val name: String = dataFormat
  }

  object TargetDataFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetDataFormat) {
    val name: String = targetDataFormat
  }

  object SchemaParameter
      extends OptionalQueryParamDecoderMatcher[String](schema) {
    val name: String = schema
  }

  object SchemaURLParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaURL) {
    val name: String = schemaURL
  }

  object SchemaFileParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaFile) {
    val name: String = schemaFile
  }

  object SchemaFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaFormat) {
    val name: String = schemaFormat
  }

  object SchemaEngineParameter
      extends OptionalQueryParamDecoderMatcher[String](schemaEngine) {
    val name: String = schemaEngine
  }

  object TargetSchemaFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetSchemaFormat) {
    val name: String = targetSchemaFormat
  }

  object TargetSchemaEngineParameter
      extends OptionalQueryParamDecoderMatcher[String](targetSchemaEngine) {
    val name: String = targetSchemaEngine
  }

  object InferenceParameter
      extends OptionalQueryParamDecoderMatcher[String](inference) {
    val name: String = inference
  }

  object TriggerModeParameter
      extends OptionalQueryParamDecoderMatcher[String](triggerMode) {
    val name: String = triggerMode
  }

  object ShapeParameter
      extends OptionalQueryParamDecoderMatcher[String](shape) {
    val name: String = shape
  }

  object EntityParameter
      extends OptionalQueryParamDecoderMatcher[String](entity) {
    val name: String = entity
  }

  object NodeParameter extends OptionalQueryParamDecoderMatcher[String](node) {
    val name: String = node
  }

  object NodeSelectorParameter
      extends OptionalQueryParamDecoderMatcher[String](nodeSelector) {
    val name: String = nodeSelector
  }

  object ShapeMapTextParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMap) {
    val name: String = shapeMap
  }

  object ShapeMapParameterAlt
      extends OptionalQueryParamDecoderMatcher[String](shape_map) {
    val name: String = shape_map
  }

  object ShapeMapUrlParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapURL) {
    val name: String = shapeMapURL
  }

  object ShapeMapFileParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapFile) {
    val name: String = shapeMapFile
  }

  object ShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](shapeMapFormat) {
    val name: String = shapeMapFormat
  }

  object TargetShapeMapFormatParameter
      extends OptionalQueryParamDecoderMatcher[String](targetShapeMapFormat) {
    val name: String = targetShapeMapFormat
  }

  object QueryParameter
      extends OptionalQueryParamDecoderMatcher[String](query) {
    val name: String = query
  }

  object QueryURLParameter
      extends OptionalQueryParamDecoderMatcher[String](queryURL) {
    val name: String = queryURL
  }

  object QueryFileParameter
      extends OptionalQueryParamDecoderMatcher[String](queryFile) {
    val name: String = queryFile
  }

  object EndpointParameter
      extends OptionalQueryParamDecoderMatcher[String](endpoint) {
    val name: String = endpoint
  }

  object ActiveDataTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeDataTab) {
    val name: String = activeDataTab
  }

  object ActiveSchemaTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeSchemaTab) {
    val name: String = activeSchemaTab
  }

  object ActiveShapeMapTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeShapeMapTab) {
    val name: String = activeShapeMapTab
  }

  object ActiveQueryTabParameter
      extends OptionalQueryParamDecoderMatcher[String](activeQueryTab) {
    val name: String = activeQueryTab
  }

  object WdEntityParameter extends QueryParamDecoderMatcher[String](wdEntity) {
    val name: String = wdEntity
  }

  object WdSchemaParameter extends QueryParamDecoderMatcher[String](wdSchema) {
    val name: String = wdSchema
  }

  object WithDotParameter
      extends OptionalQueryParamDecoderMatcher[Boolean](withDot) {
    val name: String = withDot
  }

  object OptView extends OptionalQueryParamDecoderMatcher[String](view) {
    val name: String = view
  }

  object ExamplesParameter
      extends OptionalQueryParamDecoderMatcher[String](examples) {
    val name: String = examples
  }

  object OptExamplesParameter
      extends OptionalQueryParamDecoderMatcher[String](examples) {
    val name: String = examples
  }

  object ManifestURLParameter
      extends OptionalQueryParamDecoderMatcher[String](manifestURL) {
    val name: String = manifestURL
  }

  object LanguageParameter extends QueryParamDecoderMatcher[String](language) {
    val name: String = language
  }

  object LabelParameter extends QueryParamDecoderMatcher[String](label) {
    val name: String = label
  }

  object UrlParameter extends QueryParamDecoderMatcher[String](url) {
    val name: String = url
  }

  object UrlCodeParameter extends QueryParamDecoderMatcher[String](urlCode) {
    val name: String = urlCode
  }

  object HostNameParameter extends QueryParamDecoderMatcher[String](hostname) {
    val name: String = hostname
  }

  object LimitParameter
      extends OptionalQueryParamDecoderMatcher[String](limit) {
    val name: String = limit
  }

  object ContinueParameter
      extends OptionalQueryParamDecoderMatcher[String](continue) {
    val name: String = continue
  }

}
