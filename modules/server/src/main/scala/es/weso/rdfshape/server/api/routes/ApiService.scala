package es.weso.rdfshape.server.api.routes

/** Simple interface all API services should comply with
  * By convention, all services that are [[ApiService]]s are mounted behind
  * the "/api"
  * @see [[api]]
  */
trait ApiService {

  /** The service's characteristic verb, e.g.: "permalink", "data", "wikidata"...
    */
  val verb: String
}
