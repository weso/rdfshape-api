package es.weso.rdfshape.server.api.merged
import es.weso.rdf.RDFReasoner
// import cats._
import cats.effect._
import cats.implicits._
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdfshape.server.api.merged.DataElement._
import io.circe._
import io.circe.parser._
import io.circe.syntax._

case class CompoundData(elems: List[DataElement]) {
  def toRDF: IO[Resource[IO, RDFReasoner]] = {
    val rs = elems.map(_.toRDF).sequence
    def combine(
        ls: List[Resource[IO, RDFAsJenaModel]]
    ): Resource[IO, List[RDFAsJenaModel]] = ls.sequence
    val v = rs.flatMap(lsRs =>
      IO(combine(lsRs).evalMap(ls => MergedModels.fromList(ls)))
    )
    v
  }
}

object CompoundData {

  def fromString(str: String): Either[String, CompoundData] = for {
    json <- parse(str).leftMap(pe =>
      s"CompoundData.fromString: error parsing $str as JSON: ${pe}"
    )
    cd <- json
      .as[CompoundData]
      .leftMap(de =>
        s"CompoundData.fromString: error decoding json to compoundData: ${de}\nJSON obtained: \n${json.spaces2}"
      )
  } yield cd

  implicit val encodeCompoundData: Encoder[CompoundData] =
    new Encoder[CompoundData] {
      final def apply(a: CompoundData): Json =
        Json.fromValues(a.elems.map(_.asJson))
    }

  implicit val decodeCompoundData: Decoder[CompoundData] =
    new Decoder[CompoundData] {
      final def apply(c: HCursor): Decoder.Result[CompoundData] = {
        c.values match {
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

}
