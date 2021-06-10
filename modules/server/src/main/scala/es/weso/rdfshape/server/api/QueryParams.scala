package es.weso.rdfshape.server.api

import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

object QueryParams {
  lazy val data             = "data"
  lazy val compoundData     = "compoundData"
  lazy val dataURL          = "dataURL"
  lazy val endpoint         = "endpoint"
  lazy val endpoints        = "endpoints"
  lazy val dataFormat       = "dataFormat"
  lazy val targetDataFormat = "targetDataFormat"
  lazy val schema           = "schema"
  lazy val entity           = "entity"
  lazy val node             = "node"
  lazy val withDot          = "withDot"
  lazy val schemaURL        = "schemaURL"
  lazy val schemaFormat     = "schemaFormat"
  lazy val shape            = "shape"
  lazy val url              = "url"
  lazy val urlCode          = "urlCode"
  lazy val hostname         = "hostname"
  object DataParameter extends OptionalQueryParamDecoderMatcher[String](data)
  object OptDataParam  extends OptionalQueryParamDecoderMatcher[String](data)
  object OptEndpointParam
      extends OptionalQueryParamDecoderMatcher[String](endpoint)
  object OptDataURLParam
      extends OptionalQueryParamDecoderMatcher[String](dataURL)
  object DataFormatParam
      extends OptionalQueryParamDecoderMatcher[String](dataFormat)
  object CompoundDataParam
      extends OptionalQueryParamDecoderMatcher[String](compoundData)
  object TargetDataFormatParam
      extends OptionalQueryParamDecoderMatcher[String](targetDataFormat)
  object OptSchemaParam extends OptionalQueryParamDecoderMatcher[String](schema)
  object OptEntityParam extends OptionalQueryParamDecoderMatcher[String](entity)
  object OptNodeParam   extends OptionalQueryParamDecoderMatcher[String](node)
  object OptWithDotParam
      extends OptionalQueryParamDecoderMatcher[Boolean](withDot)
  object SchemaURLParam
      extends OptionalQueryParamDecoderMatcher[String](schemaURL)
  object SchemaFormatParam
      extends OptionalQueryParamDecoderMatcher[String](schemaFormat)
  object OptNodeSelectorParam
      extends OptionalQueryParamDecoderMatcher[String]("nodeSelector")
  object SchemaEngineParam
      extends OptionalQueryParamDecoderMatcher[String]("schemaEngine")
  object OptView extends OptionalQueryParamDecoderMatcher[String]("view")
  object TargetSchemaFormatParam
      extends OptionalQueryParamDecoderMatcher[String]("targetSchemaFormat")
  object TargetSchemaEngineParam
      extends OptionalQueryParamDecoderMatcher[String]("targetSchemaEngine")
  object OptTriggerModeParam
      extends OptionalQueryParamDecoderMatcher[String]("triggerMode")
  object NodeParam  extends OptionalQueryParamDecoderMatcher[String](node)
  object ShapeParam extends OptionalQueryParamDecoderMatcher[String](shape)
//   object NameParam extends OptionalQueryParamDecoderMatcher[String]("name")
  object ShapeMapParameter
      extends OptionalQueryParamDecoderMatcher[String]("shapeMap")
  object ShapeMapParameterAlt
      extends OptionalQueryParamDecoderMatcher[String]("shape-map")
  object ShapeMapURLParameter
      extends OptionalQueryParamDecoderMatcher[String]("shapeMapURL")
  object ShapeMapFileParameter
      extends OptionalQueryParamDecoderMatcher[String]("shapeMapFile")
  object ShapeMapFormatParam
      extends OptionalQueryParamDecoderMatcher[String]("shapeMapFormat")
  object SchemaEmbedded
      extends OptionalQueryParamDecoderMatcher[Boolean]("schemaEmbedded")
  object InferenceParam
      extends OptionalQueryParamDecoderMatcher[String]("inference")
  object ExamplesParam
      extends OptionalQueryParamDecoderMatcher[String]("examples")
  object ManifestURLParam
      extends OptionalQueryParamDecoderMatcher[String]("manifestURL")
  object OptExamplesParam
      extends OptionalQueryParamDecoderMatcher[String]("examples")
  object OptQueryParam extends OptionalQueryParamDecoderMatcher[String]("query")
  object OptActiveDataTabParam
      extends OptionalQueryParamDecoderMatcher[String]("activeDataTab")
  object OptActiveSchemaTabParam
      extends OptionalQueryParamDecoderMatcher[String]("activeSchemaTab")
  object OptActiveShapeMapTabParam
      extends OptionalQueryParamDecoderMatcher[String]("activeShapeMapTab")
  object OptActiveQueryTabParam
      extends OptionalQueryParamDecoderMatcher[String]("activeQueryTab")
  object WdEntityParam extends QueryParamDecoderMatcher[String]("wdEntity")
  object WdSchemaParam extends QueryParamDecoderMatcher[String]("wdSchema")
  object LanguageParam extends QueryParamDecoderMatcher[String]("language")
  object LabelParam    extends QueryParamDecoderMatcher[String]("label")
  object UrlParam      extends QueryParamDecoderMatcher[String](url)
  object UrlCodeParam  extends QueryParamDecoderMatcher[String](urlCode)
  object HostNameParam extends QueryParamDecoderMatcher[String](hostname)
  object LimitParam    extends OptionalQueryParamDecoderMatcher[String]("limit")
  object ContinueParam
      extends OptionalQueryParamDecoderMatcher[String]("continue")

}
