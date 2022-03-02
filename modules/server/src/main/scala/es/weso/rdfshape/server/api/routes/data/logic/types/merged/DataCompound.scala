package es.weso.rdfshape.server.api.routes.data.logic.types.merged

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.{DataFormat, Mixed}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.types.{
  Data,
  DataCompanion,
  DataSingle
}
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.ContentParameter
import io.circe._
import io.circe.parser._
import io.circe.syntax._

/** Data class representing the merge of several RDF data into a single compound
  *
  * @param elements List of the individual({@linkplain DataSingle SimpleData}) conforming a CompoundData instance
  */
case class DataCompound(elements: List[Data]) extends Data with LazyLogging {

  // Non empty content
  assume(elements.nonEmpty, "Could not build the RDF from an empty set of data")
  // Valid source
  override val source: DataSource = DataSource.COMPOUND
  // Use the first element's format as placeholder
  override val format: DataFormat = Mixed

  /** Return the compound of all the inner element's data appended to each other.
    *
    * @note If one element's data cannot be computed, returns none.
    */
  override lazy val fetchedContents: Either[String, String] = {
    val definedElements =
      elements.map(_.fetchedContents).filter(_.isRight).map(_.toOption.get)
    // If all elements' raw data was computed...
    if(elements.length == definedElements.length)
      Right(definedElements.mkString("\n"))
    else Left("Could not parse compound data")
  }

  // Fetched contents successfully
  assume(
    fetchedContents.isRight,
    fetchedContents.left.getOrElse("Unknown error creating the data")
  )

  override val raw: String = fetchedContents.toOption.get

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

  /** Recursively process the data in the compound to extract all individual
    * RDF Jena models to a single list
    *
    * @return List of RDF Jena models in each of the elements of the compound
    */
  private def getJenaModels: List[IO[Resource[IO, RDFAsJenaModel]]] = {
    elements.flatMap {
      // Single data: straight extraction
      case sd: DataSingle => List(sd.toRdf())
      case cd: DataCompound =>
        cd.getJenaModels // Compound data: recursive extraction
    }
  }
}

private[api] object DataCompound
    extends DataCompanion[DataCompound]
    with LazyLogging {

  /** Encoder used to transform CompoundData instances to JSON values
    */
  override implicit val encode: Encoder[DataCompound] =
    (data: DataCompound) => Json.fromValues(data.elements.map(_.asJson))

  /** Decoder used to extract CompoundData instances from JSON values
    */
  override implicit val decode: Decoder[Either[String, DataCompound]] =
    (cursor: HCursor) => {
      for {
        // Content must exist and be an array of JSON items
        content <- cursor
          .downField(ContentParameter.name)
          .as[Json]

        result <- content.hcursor.values match {
          // If no values in the content array, return error
          case None =>
            "Empty dataset supplied for compound data"
              .asLeft[DataCompound]
              .asRight[
                DecodingFailure // Wrap in either to create a DecodingResult
              ]
          // Else, process the items cumulatively
          case Some(items) =>
            for {
              maybeItems <- items.toList
                .map(_.as[Either[String, Data]])
                .sequence
              // Scan the list for the first error parsing the data elements
              ret = maybeItems.find(_.isLeft) match {
                // Error found, return it
                case Some(err) =>
                  err.left
                    .getOrElse("Unknown error parsing compound data")
                    .asLeft[DataCompound]
                // No errors, assume all items are OK and create the compound
                case None =>
                  val items = maybeItems.flatMap(_.toOption)
                  DataCompound(items).asRight[String]
              }
            } yield ret
        }
      } yield result
    }

  /** Try to build a CompoundData instance from a JSON string
    *
    * @param jsonStr String in JSON format containing the information to build the CompoundData
    * @return Either a new CompoundData instance or an error message
    * @note Internally resorts to the decoding method in this class
    */
  def fromJsonString(jsonStr: String): Either[String, DataCompound] = {
    val decodeStringResult = for {
      json <- parse(jsonStr).leftMap(parseError =>
        s"Error parsing $jsonStr as JSON: $parseError"
      )
      compoundData <- json
        .as[Either[String, DataCompound]]
        .leftMap(decodeError =>
          s"Error decoding json to compoundData: $decodeError\nJSON obtained: \n${json.spaces2}"
        )
    } yield compoundData

    decodeStringResult.flatten
  }
}
