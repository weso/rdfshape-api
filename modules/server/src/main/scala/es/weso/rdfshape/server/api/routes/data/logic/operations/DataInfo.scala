package es.weso.rdfshape.server.api.routes.data.logic.operations

import cats.effect.IO
import es.weso.rdf.PrefixMap
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.routes.data.logic.operations.DataInfo.successMessage
import es.weso.rdfshape.server.api.routes.data.logic.types.Data
import es.weso.rdfshape.server.utils.json.JsonUtils._
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

/** Data class representing the output of a data-information operation
  *
  * @param inputData          RDF input data (contains content and format information)
  * @param predicates         List of predicates in the RDF input
  * @param numberOfStatements Number of statements in the RDF input
  * @param prefixMap          Prefix map in the RDF input
  */

final case class DataInfo private (
    override val inputData: Data,
    numberOfStatements: Int,
    prefixMap: PrefixMap,
    predicates: Set[IRI]
) extends DataOperation(successMessage, inputData) {}

/** Static utilities to obtain information about RDF data
  */
private[api] object DataInfo {

  private val successMessage = "Well formed RDF"

  /** Given an input data, get information about it
    *
    * @param data Input Data object of any type (Simple, Compound...)
    * @return Either a DataInfo object about the input data or an error message
    */

  def dataInfo(data: Data): IO[DataInfo] = for {
    rdf <- data.toRdf()
    info <- rdf.use(rdf =>
      for {
        nStatements <- rdf.getNumberOfStatements()
        predicates  <- rdf.predicates().compile.toList
        prefixMap   <- rdf.getPrefixMap
      } yield (nStatements, predicates, prefixMap)
    )

    (nStatements, predicates, prefixMap) = info

  } yield DataInfo(
    inputData = data,
    numberOfStatements = nStatements,
    predicates = predicates.toSet,
    prefixMap = prefixMap
  )

  implicit val encodeResult: Encoder[DataInfo] =
    (dataInfo: DataInfo) => {

      val resultJson: Json = Json.fromFields(
        List(
          ("numberOfStatements", dataInfo.numberOfStatements.asJson),
          ("format", dataInfo.inputData.format.asJson),
          ("prefixMap", prefixMap2Json(dataInfo.prefixMap)),
          (
            "predicates",
            Json.fromValues(
              dataInfo.predicates.map(iri2Json(_, Some(dataInfo.prefixMap)))
            )
          )
        )
      )

      Json.fromFields(
        List(
          ("message", Json.fromString(dataInfo.successMessage)),
          ("data", dataInfo.inputData.asJson),
          ("result", resultJson)
        )
      )
    }
}
