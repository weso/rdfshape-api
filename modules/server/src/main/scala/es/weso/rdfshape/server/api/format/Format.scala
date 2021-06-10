package es.weso.rdfshape.server.api.format

import org.http4s.MediaType

trait Format {
  val name: String
  val mimeType: MediaType

//   def availableFormats[F <: Format]: List[F]
//   def default[F<:Format]: F

  override def toString: String = s"Format $name"

}
