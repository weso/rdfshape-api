package es.weso.rdfshape.server.api.definitions

import es.weso.rdf.InferenceEngine
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.SchemaFormat
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  ShapeMapFormat
}
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.{
  TriggerMode,
  TriggerModeType
}
import es.weso.schema.{Schemas, Schema => SchemaW}
import es.weso.shapemaps.ShapeMap
import es.weso.utils.FileUtils
import org.http4s.Uri
import org.http4s.implicits.http4sLiteralsSyntax

/** Global definitions used in the API
  */
case object ApiDefinitions {

  lazy val localBase: IRI = IRI(FileUtils.currentFolderURL)

  /** API route inside the web server
    */
  val api = "api"

  /** [[List]] of [[DataFormat]]s accepted by the application
    */
  val availableDataFormats: List[DataFormat] = DataFormat.availableFormats

  /** [[List]] of [[SchemaFormat]]s accepted by the application
    */
  val availableSchemaFormats: List[SchemaFormat] = SchemaFormat.availableFormats

  /** [[List]] of [[SchemaW]]s used by the application
    */
  val availableSchemaEngines: List[SchemaW] = Schemas.availableSchemas

  /** [[List]] of [[ShapeMapFormat]]s accepted by the application
    */
  val availableShapeMapFormats: List[ShapeMapFormat] =
    ShapeMap.formats
      .map(f => ShapeMapFormat.fromString(f))
      .filter(_.isRight)
      .map(_.getOrElse(ShapeMapFormat.defaultFormat))

  /** [[List]] of [[TriggerMode]]s accepted by the application, by name
    *
    * @note Must be coherent with [[es.weso.schema.Schemas.availableTriggerModes]]
    */
  val availableTriggerModes: List[String] =
    List(TriggerModeType.SHAPEMAP, TriggerModeType.TARGET_DECLARATIONS)

  /** [[List]] of [[InferenceEngine]]s accepted by the application
    */
  val availableInferenceEngines: List[InferenceEngine] =
    InferenceEngine.availableInferenceEngines

  /** [[IRI]] used as base for nodes created internally
    */
  val relativeBase: Some[IRI] = Some(IRI("internal://base/"))

  /** [[Uri]] representation of Wikidata's base URL
    */
  val wikidataUri: Uri = uri"https://www.wikidata.org"

  /** [[String]] representation of Wikidata's base URL
    */
  val wikidataUrl: String = wikidataUri.renderString

  /** [[Uri]] representation of Wikidata's SPARQL endpoint
    */
  val wikidataQueryUri: Uri = uri"https://query.wikidata.org/sparql"

}
