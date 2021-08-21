package es.weso.rdfshape.server.api.routes.data.logic

import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.PrefixMap
import es.weso.schema.DataFormats
import io.circe.Json

/** Static utilities used by the {@link es.weso.rdfshape.server.api.routes.data.service.DataService}
  * to operate on RDF data
  */
private[api] object DataOperations extends LazyLogging {

  /** @param df Data format
    * @return The given data format or the default one in case none was provided
    */
  def dataFormatOrDefault(df: Option[String]): String =
    df.getOrElse(DataFormats.defaultFormatName)

  /** Convert a given prefix map to JSON format for API operations
    *
    * @param prefixMap Input prefix map
    * @return JSON representation of the prefix map
    */
  private[api] def prefixMap2Json(prefixMap: PrefixMap): Json = {
    Json.fromFields(prefixMap.pm.map { case (prefix, iri) =>
      (prefix.str, Json.fromString(iri.getLexicalForm))
    })
  }

}
