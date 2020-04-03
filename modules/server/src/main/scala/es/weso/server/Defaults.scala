package es.weso.server

import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.schema.Schemas
import es.weso.shapeMaps.ShapeMap
import es.weso.schema._
import es.weso.server.helper.DataFormat
import cats.effect.IO

object Defaults {

  val availableDataFormats = DataFormat.availableDataFormats
  val defaultDataFormat = DataFormat.default
  val availableSchemaFormats = Schemas.availableFormats
  val defaultSchemaFormat = Schemas.defaultSchemaFormat
  val availableSchemaEngines = Schemas.availableSchemaNames
  val defaultSchemaEngine = Schemas.defaultSchemaName
  val availableTriggerModes = Schemas.availableTriggerModes
  val defaultTriggerMode = ShapeMapTrigger(ShapeMap.empty).name
  val availableInferenceEngines = RDFAsJenaModel.empty.map(_.availableInferenceEngines).unsafeRunSync
  val defaultSchemaEmbedded = false
  val defaultInference = "None"
  val defaultActiveDataTab = "#dataTextArea"
  val defaultActiveSchemaTab = "#schemaTextArea"
  val defaultActiveQueryTab = "#queryTextArea"
  val defaultShapeMapFormat = ShapeMap.defaultFormat
  val availableShapeMapFormats = ShapeMap.formats
  val defaultActiveShapeMapTab = "#shapeMapTextArea"
  val defaultShapeLabel = IRI("Shape")
  val relativeBase = Some(IRI("internal://base/"))

}