package es.weso.rdfshape.server.api

import io.circe.Decoder

/** This trait defines an operation performed by a route in a service
  * and the way to decode the incoming data into the required object
  *
  * All errors occurred during object instantiation will be mapped to
  * DecodingErrors
  *
  * @tparam C Type into which the client data will be decoded for usage in this route
  */
trait ServiceRouteOperation[C] {

  /** Decoder in charge of converting the data sent by the client to a
    * usable domain structure
    */
  implicit val decoder: Decoder[C]
}
