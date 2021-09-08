package es.weso.rdfshape.server.api.routes.data.logic

import cats.data.EitherT
import cats.effect.IO
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{PrefixMap, RDFReasoner}
import es.weso.rdfshape.server.api.format._
import es.weso.rdfshape.server.api.routes.data.logic.DataOperations.prefixMap2Json
import es.weso.rdfshape.server.utils.json.JsonUtils._
import es.weso.utils.IOUtils.{either2es, io2es}
import io.circe.Json

/** Data class representing the output of an "information" operation
  *
  * @param message            Output informational message after processing. Used in case of error.
  * @param data               RDF input data
  * @param dataFormat         RDF input data format
  * @param predicates         List of predicates of the RDF input
  * @param numberOfStatements Number of statements in the RDF input
  * @param prefixMap          Prefix map of the RDF input
  */
final case class DataInfo private (
    message: String,
    data: Option[String],
    dataFormat: Option[DataFormat],
    predicates: Option[Set[IRI]],
    numberOfStatements: Option[Int],
    prefixMap: Option[PrefixMap]
) {

  /** Prefix map: defaults to empty.
    */
  lazy val pm: PrefixMap = prefixMap.getOrElse(PrefixMap.empty)

  /** Convert an information result to its JSON representation
    *
    * @return JSON information of the extraction result
    */
  def toJson: Json = {
    Json.fromFields(
      List(("message", Json.fromString(message))) ++
        maybeField("data", data, Json.fromString) ++
        maybeField(
          "dataFormat",
          dataFormat,
          (df: DataFormat) => Json.fromString(df.name)
        ) ++
        maybeField("numberOfStatements", numberOfStatements, Json.fromInt) ++
        maybeField("prefixMap", prefixMap, prefixMap2Json) ++
        maybeField(
          "predicates",
          predicates,
          (preds: Set[IRI]) => Json.fromValues(preds.map(iri2Json))
        )
    )
  }

  /** @param iri IRI to be converted
    * @return JSON representation of the IRI
    */
  private def iri2Json(iri: IRI): Json = {
    Json.fromString(pm.qualifyIRI(iri))
  }

}

/** Static utilities to obtain information about RDF data
  */
object DataInfo {

  /** Message attached to the result when created successfully
    */
  val successMessage = "Well formed RDF"

  /** For a given RDF input (plain text), return information about it
    *
    * @param data          Input data string
    * @param dataFormatStr Input data format
    * @return Information about the input RDF: statements, well-formed, etc.
    */
  def dataInfoFromString(
      data: String,
      dataFormatStr: String
  ): IO[Either[String, DataInfo]] = {
    val either: EitherT[IO, String, DataInfo] = for {
      dataFormat <- either2es(DataFormat.fromString(dataFormatStr))
      json <- io2es(
        RDFAsJenaModel
          .fromChars(data, dataFormat.name)
          .flatMap(
            _.use(rdf => dataInfoFromRdf(rdf, Some(data), Some(dataFormat)))
          )
      )
      ret <- EitherT.fromEither[IO](json)
    } yield ret

    either.fold(e => Left(e), d => Right(d))
  }

  /** For a given RDF input, return information about it
    *
    * @param rdf        Input RDF
    * @param data       Input data string
    * @param dataFormat Input data format
    * @return Information about the input RDF: statements, well-formed, etc.
    */
  def dataInfoFromRdf(
      rdf: RDFReasoner,
      data: Option[String],
      dataFormat: Option[DataFormat]
  ): IO[Either[String, DataInfo]] = {
    val either: IO[Either[Throwable, DataInfo]] = (for {
      numberOfStatements <- rdf.getNumberOfStatements()
      predicates         <- rdf.predicates().compile.toList
      pm                 <- rdf.getPrefixMap
    } yield DataInfo.fromData(
      data,
      dataFormat,
      predicates.toSet,
      numberOfStatements,
      pm
    )).attempt
    either.map(
      _.fold(e => Left(e.getMessage), r => Right(r))
    )
  }

  /** @return A DataInfoResult, given all the parameters needed to build it (input, predicates, etc.)
    */
  def fromData(
      data: Option[String],
      dataFormat: Option[DataFormat],
      predicates: Set[IRI],
      numberOfStatements: Int,
      prefixMap: PrefixMap
  ): DataInfo =
    DataInfo(
      successMessage,
      data,
      dataFormat,
      Some(predicates),
      Some(numberOfStatements),
      Some(prefixMap)
    )
}
