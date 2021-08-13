package es.weso.rdfshape.server.api.routes

import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.{DataFormat, SchemaFormat}
import es.weso.schema.{Schemas, ShapeMapTrigger}
import es.weso.shapemaps.ShapeMap

/** Global definitions used in the API
  */
object ApiDefinitions {

  /** API route inside the web server
    */
  val api = "api"
}

/** Application-wide defaults
  */
object Defaults {

  val availableDataFormats: List[DataFormat]     = DataFormat.availableFormats
  val defaultDataFormat: DataFormat              = DataFormat.defaultFormat
  val availableSchemaFormats: List[SchemaFormat] = SchemaFormat.availableFormats
  val defaultSchemaFormat: SchemaFormat          = SchemaFormat.defaultFormat
  val availableSchemaEngines: List[String]       = Schemas.availableSchemaNames
  val defaultSchemaEngine: String                = Schemas.defaultSchemaName
  val availableTriggerModes: List[String]        = Schemas.availableTriggerModes
  val defaultTriggerMode: String                 = ShapeMapTrigger(ShapeMap.empty).name
  val availableInferenceEngines = List(
    "NONE",
    "RDFS",
    "OWL"
  ) // TODO: Obtain from RDFAsJenaModel.empty.map(_.availableInferenceEngines).unsafeRunSync
  val defaultSchemaEmbedded                  = false
  val defaultInference: String               = availableInferenceEngines.head
  val defaultActiveDataTab                   = "#dataTextArea"
  val defaultActiveSchemaTab                 = "#schemaTextArea"
  val defaultActiveQueryTab                  = "#queryTextArea"
  val defaultShapeMapFormat: String          = ShapeMap.defaultFormat
  val availableShapeMapFormats: List[String] = ShapeMap.formats
  val defaultActiveShapeMapTab               = "#shapeMapTextArea"
  val defaultShapeLabel: IRI                 = IRI("Shape")
  val relativeBase: Some[IRI]                = Some(IRI("internal://base/"))

}
