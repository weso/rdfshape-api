package es.weso.server
import es.weso.server.format.DataFormat

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
