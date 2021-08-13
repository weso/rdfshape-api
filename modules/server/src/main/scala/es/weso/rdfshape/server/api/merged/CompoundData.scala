package es.weso.rdfshape.server.api.merged

import cats.effect._
import cats.implicits._
import es.weso.rdf.RDFReasoner
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.merged.DataElement._
import io.circe._
import io.circe.parser._
import io.circe.syntax._

/** Data class representing the merge of several RDF data into a single compound
  *
  * @param elements List of the individual DataElements conforming a CompoundData instance
  */
case class CompoundData(elements: List[DataElement]) {
  def toRDF: IO[Resource[IO, RDFReasoner]] = {
    val rs = elements.map(_.toRDF).sequence

    def combine(
        ls: List[Resource[IO, RDFAsJenaModel]]
    ): Resource[IO, List[RDFAsJenaModel]] = ls.sequence

    /** Whole compound value resulting from merging the individual elements
      */
    val value = rs.flatMap(lsRs =>
      IO(combine(lsRs).evalMap(ls => MergedModels.fromList(ls)))
    )
    value
  }
}

object CompoundData {

  def fromString(str: String): Either[String, CompoundData] = for {
    json <- parse(str).leftMap(pe =>
      s"CompoundData.fromString: error parsing $str as JSON: $pe"
    )
    cd <- json
      .as[CompoundData]
      .leftMap(de =>
        s"CompoundData.fromString: error decoding json to compoundData: $de\nJSON obtained: \n${json.spaces2}"
      )
  } yield cd

  /** Encoder used to transform CompoundData instances to JSON values
    */
  implicit val encodeCompoundData: Encoder[CompoundData] =
    (a: CompoundData) => Json.fromValues(a.elements.map(_.asJson))

  /** Decoder used to extract CompoundData instances from JSON values
    */
  implicit val decodeCompoundData: Decoder[CompoundData] =
    (cursor: HCursor) => {
      cursor.values match {
        case None =>
          DecodingFailure("Empty list for compound data", List())
            .asLeft[CompoundData]
        case Some(vs) =>
          val xs: Decoder.Result[List[DataElement]] =
            vs.toList.map(_.as[DataElement]).sequence
          xs.map(CompoundData(_))
      }
    }

}
