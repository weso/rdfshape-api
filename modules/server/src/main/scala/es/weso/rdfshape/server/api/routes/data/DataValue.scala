package es.weso.rdfshape.server.api.routes.data

import es.weso.rdfshape.server.api.format.DataFormat

case class DataValue(
    data: Option[String],
    dataURL: Option[String],
    currentDataFormat: DataFormat,
    availableDataFormats: List[DataFormat],
    currentInferenceEngine: String,
    availableInferenceEngines: List[String],
    endpoint: Option[String],
    activeDataTab: String
)
