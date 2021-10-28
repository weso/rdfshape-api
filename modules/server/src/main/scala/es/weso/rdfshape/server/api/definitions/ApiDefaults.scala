package es.weso.rdfshape.server.api.definitions

import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, NONE}
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  RDFFormat,
  SchemaFormat,
  ShapeMapFormat
}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource.SchemaSource
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.ShapeMapSource
import es.weso.schema.{Schema, Schemas, ShapeMapTrigger}
import es.weso.shapemaps.ShapeMap

/** Application-wide defaults
  */
case object ApiDefaults {
  val availableDataFormats: List[DataFormat]     = DataFormat.availableFormats
  val defaultDataFormat: DataFormat              = DataFormat.defaultFormat
  val defaultRdfFormat: RDFFormat                = RDFFormat.defaultFormat
  val availableSchemaFormats: List[SchemaFormat] = SchemaFormat.availableFormats
  val defaultSchemaFormat: SchemaFormat          = SchemaFormat.defaultFormat
  val defaultSchemaFormatName: String            = defaultSchemaFormat.name
  val availableSchemaEngines: List[String]       = Schemas.availableSchemaNames
  val defaultSchemaEngine: Schema                = Schemas.defaultSchema
  val defaultSchemaEngineName: String            = defaultSchemaEngine.name
  val availableTriggerModes: List[String]        = Schemas.availableTriggerModes
  val defaultTriggerMode: String                 = ShapeMapTrigger(ShapeMap.empty).name
  val availableInferenceEngines = List(
    "NONE",
    "RDFS",
    "OWL"
  ) // TODO: Obtain from RDFAsJenaModel.empty.map(_.availableInferenceEngines).unsafeRunSync
  val defaultSchemaEmbedded                   = false
  val defaultInferenceEngine: InferenceEngine = NONE
  val defaultInferenceEngineName: String      = defaultInferenceEngine.name
  val defaultActiveDataSource: DataSource     = DataSource.defaultDataSource
  val defaultActiveSchemaSource: SchemaSource =
    SchemaSource.defaultSchemaSource
  val defaultActiveShapeMapSource: ShapeMapSource =
    ShapeMapSource.defaultShapeMapSource
  val defaultShapeMapFormat: ShapeMapFormat  = ShapeMapFormat.defaultFormat
  val availableShapeMapFormats: List[String] = ShapeMap.formats
  val defaultActiveShapeMapTab               = "#shapeMapTextArea"
  val defaultShapeLabel: IRI                 = IRI("Shape")
  val relativeBase: Some[IRI]                = Some(IRI("internal://base/"))

}
