package es.weso.server.merged
import es.weso.rdf.RDFReasoner
import cats._
import cats.data._
import cats.implicits._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import DataElement._
import cats.effect._


case class CompoundData(elems: List[DataElement]) {
    def toRDF: Resource[IO,RDFReasoner] = for { 
        vs <- elems.map(_.toRDF).sequence
        merged <- Resource.liftF(MergedModels.fromList(vs))
    } yield merged
}

object CompoundData {

 def fromString(str: String): Either[String,CompoundData] = for {
     json <- parse(str).leftMap(pe => s"CompoundData.fromString: error parsing $str as JSON: ${pe}")
     cd <- json.as[CompoundData].leftMap(de => s"CompoundData.fromString: error decoding json to compoundData: ${de}\nJSON obtained: \n${json.spaces2}")
 } yield cd

 implicit val encodeCompoundData: Encoder[CompoundData] = new Encoder[CompoundData] {
  final def apply(a: CompoundData): Json = Json.fromValues(a.elems.toList.map(_.asJson))
 }

 implicit val decodeCompoundData: Decoder[CompoundData] = new Decoder[CompoundData] {
  final def apply(c: HCursor): Decoder.Result[CompoundData] = {
      c.values match {
          case None => DecodingFailure("Empty list for compound data", List()).asLeft[CompoundData]
          case Some(vs) => { 
              val xs: Decoder.Result[List[DataElement]] = vs.toList.map(_.as[DataElement]).sequence 
              xs.map(CompoundData(_))
        }
      }
  }
 }

}