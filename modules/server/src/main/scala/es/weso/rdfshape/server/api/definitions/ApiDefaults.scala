package es.weso.rdfshape.server.api.definitions

import es.weso.rdf.nodes.IRI
import es.weso.rdf.{InferenceEngine, NONE}
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.{
  SchemaFormat,
  ShaclFormat
}
import es.weso.rdfshape.server.api.format.dataFormats.{
  DataFormat,
  RdfFormat,
  ShapeMapFormat
}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource
import es.weso.rdfshape.server.api.routes.schema.logic.SchemaSource.SchemaSource
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource
import es.weso.rdfshape.server.api.routes.shapemap.logic.ShapeMapSource.ShapeMapSource
import es.weso.schema.{
  Schemas,
  ShapeMapTrigger,
  ValidationTrigger,
  Schema => SchemaW
}
import es.weso.shapemaps.ShapeMap

/** Application-wide defaults
  */
case object ApiDefaults {

  /** [[DataFormat]] used when the format can be omitted or is needed but none was provided
    */
  val defaultDataFormat: DataFormat = DataFormat.defaultFormat

  /** [[RdfFormat]] used when the format can be omitted or is needed but none was provided
    */
  val defaultRdfFormat: RdfFormat = RdfFormat.defaultFormat

  /** [[SchemaFormat]] used when the format can be omitted or is needed but none was provided
    */
  val defaultSchemaFormat: SchemaFormat = ShaclFormat.defaultFormat

  /** [[ShapeMapFormat]] used when the format can be omitted or is needed but none was provided
    */
  val defaultShapeMapFormat: ShapeMapFormat = ShapeMapFormat.defaultFormat

  /** Schema engined ([[SchemaW]]) used when the engine can be omitted or is needed but none was provided
    */
  val defaultSchemaEngine: SchemaW = Schemas.defaultSchema

  /** [[ValidationTrigger]] used when the trigger can be omitted or is needed but none was provided
    */
  val defaultTriggerMode: ValidationTrigger = ShapeMapTrigger(
    ShapeMap.empty
  )

  /** [[InferenceEngine]] used when the engine can be omitted or is needed but none was provided
    */
  val defaultInferenceEngine: InferenceEngine = NONE

  /** [[DataSource]] used when the source can be omitted or is needed but none was provided
    */
  val defaultDataSource: DataSource = DataSource.defaultDataSource

  /** [[SchemaSource]] used when the source can be omitted or is needed but none was provided
    */
  val defaultSchemaSource: SchemaSource =
    SchemaSource.defaultSchemaSource

  /** [[ShapeMapSource]] used when the source can be omitted or is needed but none was provided
    */
  val defaultShapeMapSource: ShapeMapSource =
    ShapeMapSource.defaultShapeMapSource

  /** [[IRI]] used when the shape label can be omitted or is needed but none was provided
    */
  val defaultShapeLabel: IRI = IRI("Shape")
}
