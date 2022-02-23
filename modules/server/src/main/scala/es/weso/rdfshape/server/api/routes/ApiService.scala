package es.weso.rdfshape.server.api.routes

/** Simple interface all API services should comply with
  */
trait ApiService {

  /** The service's characteristic verb, e.g.: "permalink", "data", "wikidata"...
    */
  val verb: String
}
