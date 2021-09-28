package es.weso.rdfshape.server.api.routes.data.logic.data.merged

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.routes.data.logic.data.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.data._
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.CompoundDataParameter
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe._
import io.circe.parser._
import io.circe.syntax._

/** Data class representing the merge of several RDF data into a single compound
  *
  * @param elements List of the individual({@linkplain es.weso.rdfshape.server.api.routes.data.logic.data.SimpleData SimpleData}) conforming a CompoundData instance
  */
case class CompoundData(elements: List[Data]) extends Data with LazyLogging {

  override val dataSource: DataSource = DataSource.COMPOUND

  /** @return RDF logical model of the data contained in the compound
    */
  override def toRdf(
      relativeBase: Option[IRI] = None
  ): IO[Resource[IO, RDFReasoner]] = {
    val jenaModels = getJenaModels.sequence

    // Whole compound value resulting from merging the individual elements
    val value = jenaModels.flatMap(lsRs =>
      IO(lsRs.sequence.evalMap(ls => MergedModels.fromList(ls)))
    )
    value
  }

  /** Recursively process the data in the compound to extract all individual RDF Jena models to a single list
    *
    * @return List of RDF Jena models in each of the elements of the compound
    */
  private def getJenaModels: List[IO[Resource[IO, RDFAsJenaModel]]] = {
    elements.flatMap {
      // Single data: straight extraction
      case sd: SimpleData   => List(sd.toRdf())
      case ed: EndpointData => List(ed.toRdf())
      case cd: CompoundData =>
        cd.getJenaModels // Compound data: recursive extraction
    }
  }

}

private[api] object CompoundData
    extends DataCompanion[CompoundData]
    with LazyLogging {

  override lazy val emptyData: CompoundData = CompoundData(List())

  override def mkData(partsMap: PartsMap): IO[Either[String, CompoundData]] =
    for {
      // Parse params
      compoundData <- partsMap.optPartValue(CompoundDataParameter.name)

      // Try to create data
      maybeData: Either[String, CompoundData] =
        if(compoundData.isDefined) {
          logger.debug(
            s"RDF Data received - Compound Data: ${compoundData.get}"
          )
          CompoundData
            .fromJsonString(compoundData.get)
            .leftMap(err => s"Could not read compound data: $err")
        } else Left("No compound data provided")
    } yield maybeData

  /** Encoder used to transform CompoundData instances to JSON values
    */
  override implicit val encodeData: Encoder[CompoundData] =
    (a: CompoundData) => Json.fromValues(a.elements.map(_.asJson))

  /** Decoder used to extract CompoundData instances from JSON values
    */
  override implicit val decodeData: Decoder[CompoundData] =
    (cursor: HCursor) => {
      cursor.values match {
        case None =>
          DecodingFailure("Empty list for compound data", List())
            .asLeft[CompoundData]
        case Some(vs) =>
          val xs: Decoder.Result[List[Data]] =
            vs.toList.map(_.as[Data]).sequence
          xs.map(CompoundData(_))
      }
    }

  /** Try to build a CompoundData instance from a JSON string
    *
    * @param jsonStr String in JSON format containing the information to build the CompoundData
    * @return Either a new CompoundData instance or an error message
    * @note Internally resorts to the decoding method in this class
    */
  def fromJsonString(jsonStr: String): Either[String, CompoundData] = for {
    json <- parse(jsonStr).leftMap(parseError =>
      s"CompoundData.fromString: error parsing $jsonStr as JSON: $parseError"
    )
    cd <- json
      .as[CompoundData]
      .leftMap(decodeError =>
        s"Error decoding json to compoundData: $decodeError\nJSON obtained: \n${json.spaces2}"
      )
  } yield cd

  // 1. Compound data
  //  if(compoundData.isDefined) {
  //    logger.debug(s"RDF Data received - Compound Data: ${compoundData.get}")
  //    CompoundData
  //      .fromJsonString(compoundData.get)
  //      .leftMap(err => s"Could not read compound data: $err")
  //  }
}
