package es.weso.rdfshape.server.api.routes.data.logic.types.merged

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.types.{
  Data,
  DataCompanion,
  DataEndpoint,
  DataSingle
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.CompoundDataParameter
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe._
import io.circe.parser._
import io.circe.syntax._

/** Data class representing the merge of several RDF data into a single compound
  *
  * @param elements List of the individual({@linkplain DataSingle SimpleData}) conforming a CompoundData instance
  */
case class DataCompound(elements: List[Data]) extends Data with LazyLogging {

  /** Return the compound of all the inner element's data appended to each other.
    *
    * @note If one element's data cannot be computed, returns none.
    */
  override lazy val rawData: Either[String, String] = {
    val definedElements =
      elements.map(_.rawData).filter(_.isRight).map(_.toOption.get)
    // If all elements' raw data was computed...
    if(elements.length == definedElements.length)
      Right(definedElements.mkString("\n"))
    else Left("Could not parse compound data")
  }

  override val dataSource: DataSource = DataSource.COMPOUND
  override val format: Option[DataFormat] = {
    if(elements.forall(_.format == elements.head.format)) elements.head.format
    else None
  } // None if each element has its own format. If all elements have the same format, use that  format

  /** @return RDF logical model of the data contained in the compound
    */
  override def toRdf(
      relativeBase: Option[IRI] = None
  ): IO[Resource[IO, RDFReasoner]] = {
    val jenaModels = getJenaModels.sequence

    // Whole compound value resulting from merging the individual elements
    val value: IO[Resource[IO, RDFReasoner]] = jenaModels.flatMap(lsRs =>
      IO(lsRs.sequence.evalMap(ls => MergedModels.fromList(ls)))
    )

    value
  }

  override def toString: String = {
    elements.flatMap(_.toString).mkString("\n")
  }

  /** Recursively process the data in the compound to extract all individual RDF Jena models to a single list
    *
    * @return List of RDF Jena models in each of the elements of the compound
    */
  // TODO: The moment you use one of these resources, things crash
  private def getJenaModels: List[IO[Resource[IO, RDFAsJenaModel]]] = {
    elements.flatMap {
      // Single data: straight extraction
      case sd: DataSingle   => List(sd.toRdf())
      case ed: DataEndpoint => List(ed.toRdf())
      case cd: DataCompound =>
        cd.getJenaModels // Compound data: recursive extraction
    }
  }
}

private[api] object DataCompound
    extends DataCompanion[DataCompound]
    with LazyLogging {

  override lazy val emptyData: DataCompound = DataCompound(List())

  override def mkData(partsMap: PartsMap): IO[Either[String, DataCompound]] = {
    for {
      // Parse params
      compoundData <- partsMap.optPartValue(CompoundDataParameter.name)
      // Try to create data
      maybeData: Either[String, DataCompound] =
        if(compoundData.isDefined) {
          logger.debug(
            s"RDF Data received - Compound Data: ${compoundData.get}"
          )
          DataCompound
            .fromJsonString(compoundData.get)
            .leftMap(err => s"Could not read compound data.\n $err")
        } else Left("No compound data provided")
      /* Check if the created data is empty, then an error occurred when merging
       * the elements */
    } yield maybeData.flatMap(_.rawData.flatMap(_ => maybeData))
  }

  /** Encoder used to transform CompoundData instances to JSON values
    */
  override implicit val encodeData: Encoder[DataCompound] =
    (data: DataCompound) => Json.fromValues(data.elements.map(_.asJson))

  /** Decoder used to extract CompoundData instances from JSON values
    */
  override implicit val decodeData: Decoder[DataCompound] =
    (cursor: HCursor) => {
      cursor.values match {
        case None =>
          DecodingFailure("Empty list for compound data", List())
            .asLeft[DataCompound]
        case Some(vs) =>
          val xs: Decoder.Result[List[Data]] =
            vs.toList.map(_.as[Data]).sequence

          xs.map(DataCompound(_))
      }
    }

  /** Try to build a CompoundData instance from a JSON string
    *
    * @param jsonStr String in JSON format containing the information to build the CompoundData
    * @return Either a new CompoundData instance or an error message
    * @note Internally resorts to the decoding method in this class
    */
  def fromJsonString(jsonStr: String): Either[String, DataCompound] = for {
    json <- parse(jsonStr).leftMap(parseError =>
      s"CompoundData.fromString: error parsing $jsonStr as JSON: $parseError"
    )
    compoundData <- json
      .as[DataCompound]
      .leftMap(decodeError =>
        s"Error decoding json to compoundData: $decodeError\nJSON obtained: \n${json.spaces2}"
      )
  } yield compoundData
}
