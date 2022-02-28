package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.nodes.IRI
import es.weso.rdf.{PrefixMap, RDFReasoner}
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.DataSource._
import es.weso.rdfshape.server.api.routes.data.logic.types.merged.DataCompound
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  CompoundDataParameter,
  SourceParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}

/** Common trait to all data, whichever its nature (single, compound, endpoint...)
  */
trait Data {

  /** Lazily obtain the prefix map of this data instance
    * Needs to create the in-memory RDF model first
    */
  lazy val prefixMap: IO[PrefixMap] = for {
    model <- this.toRdf()
    pm    <- model.use(_.getPrefixMap)
  } yield pm

  /** Given the user input for the data and its source, fetch the contents
    * using the input in the way the source needs it
    * (e.g.: for URLs, fetch the input with a web request; for files,
    * decode the input; for raw data, do nothing)
    *
    * @return Either the raw data contents represented as a String,
    *         or the error occurred when trying to parse the schema
    */
  val fetchedContents: Either[String, String]

  /** Raw data value, i.e.: the text forming the schema
    *
    * @note It is safely extracted from [[fetchedContents]] after asserting
    *       the [[source]] and fetched contents are right
    */
  val raw: String

  /** Source where the data comes from
    */
  val source: DataSource

  /** Format of the data
    */
  val format: DataFormat

  /** @return A String with the raw contents or the error that made them
    *          un-parseable
    */
  override def toString: String =
    fetchedContents.fold(identity, identity)

  /** Given an RDF source of data, try to parse it and get the RDF model representation
    *
    * @return RDF logical model of the data contained
    */
  def toRdf(relativeBase: Option[IRI] = None): IO[Resource[IO, RDFReasoner]]
}

object Data extends DataCompanion[Data] {

  /** Dummy implementation meant to be overridden.
    * If called on a general [[Data]] instance, pattern match among the available data types to
    * use the correct implementation
    */
  implicit val encode: Encoder[Data] = {
    case ds: DataSingle   => DataSingle.encode(ds)
    case dc: DataCompound => DataCompound.encode(dc)
  }

  /** Dummy implementation meant to be overridden
    * If called on a general [[Data]] instance, look for the data source to
    * redirecting the decoding to the correct implementation
    *
    * @note Defaults to [[DataSingle]]'s implementation
    */
  implicit val decode: Decoder[Either[String, Data]] = (cursor: HCursor) =>
    for {
      source <- cursor
        .downField(SourceParameter.name)
        .as[DataSource]

      decoded <- source match {
        case COMPOUND          => DataCompound.decode(cursor)
        case TEXT | URL | FILE => DataSingle.decode(cursor)
        case _                 => DecodingFailure(s"Invalid data source '$source'", Nil).asLeft
      }
    } yield decoded

  /** General implementation delegating on subclasses
    */
  override def mkData(partsMap: PartsMap): IO[Either[String, Data]] = for {
    // 1. Make some checks on the parameters to distinguish between Data types
    compoundData <- partsMap.optPartValue(CompoundDataParameter.name)

    // 2. Delegate on the correct sub-class for creating the Data
    maybeData <- {
      // 1. Compound data
      if(compoundData.isDefined) DataCompound.mkData(partsMap)
      // 2. Simple data or unknown
      else DataSingle.mkData(partsMap)
    }

  } yield maybeData
}

/** Static utilities to be used with [[Data]] representations
  *
  * @tparam D Specific [[Data]] representation to be handled
  */
private[data] trait DataCompanion[D <: Data] extends LazyLogging {

  /** Encoder used to transform [[Data]] instances to JSON values
    */
  implicit val encode: Encoder[D]

  /** Decoder used to extract [[Data]] instances from JSON values
    */
  implicit val decode: Decoder[Either[String, D]]

  /** Given a request's parameters, try to extract an instance of [[Data]] (type [[D]]) from them
    *
    * @param partsMap Request's parameters
    * @return Either the [[Data]] instance or an error message
    */
  def mkData(partsMap: PartsMap): IO[Either[String, D]]
}
