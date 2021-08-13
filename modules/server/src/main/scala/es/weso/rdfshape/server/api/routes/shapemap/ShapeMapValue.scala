package es.weso.rdfshape.server.api.routes.shapemap

case class ShapeMapValue(
    shapeMap: Option[String],
    shapeMapURL: Option[String],
    currentShapeMapFormat: String,
    availableShapeMapFormats: List[String],
    activeShapeMapTab: String
)
