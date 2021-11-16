package es.weso.rdfshape.server.api.routes.data.logic.types

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.DataFormat
import es.weso.rdfshape.server.api.routes.data.logic.DataSource.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.types.merged.DataCompound
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  CompoundDataParameter,
  EndpointParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe.{Decoder, Encoder, HCursor}

/** Common trait to all data, whichever its nature (single, compound, endpoint...)
  */
trait Data {

  /** Either the raw RDF content represented as a String,
    * or the error occurred when trying to parse the data
    */
  lazy val rawData: Either[String, String] = Left("")

  /** Source where the data comes from
    */
  val dataSource: DataSource

  /** Format of the data
    */
  val format: Option[DataFormat] = None

  /** Given an RDF source of data, try to parse it and get the RDF model representation
    *
    * @return RDF logical model of the data contained
    */
  def toRdf(relativeBase: Option[IRI] = None): IO[Resource[IO, RDFReasoner]]
}

object Data extends DataCompanion[Data] {

  /** Dummy implementation meant to be overridden
    *
    * @note Resort by default to [[DataSingle]]'s empty representation
    */
  override val emptyData: Data = DataSingle.emptyData

  /** Dummy implementation meant to be overridden.
    * If called on a general [[Data]] instance, pattern match among the available data types to
    * use the correct implementation
    */
  implicit val encodeData: Encoder[Data] = {
    case ds: DataSingle   => DataSingle.encodeData(ds)
    case de: DataEndpoint => DataEndpoint.encodeData(de)
    case dc: DataCompound => DataCompound.encodeData(dc)
  }

  /** Dummy implementation meant to be overridden
    * If called on a general [[Data]] instance, pattern match among the available data types to
    * use the correct implementation
    */
  implicit val decodeData: Decoder[Data] = (cursor: HCursor) => {
    this.getClass match {
      case ds if ds == classOf[DataSingle]   => DataSingle.decodeData(cursor)
      case de if de == classOf[DataEndpoint] => DataEndpoint.decodeData(cursor)
      case dc if dc == classOf[DataCompound] => DataCompound.decodeData(cursor)
    }
  }

  /** General implementation delegating on subclasses
    */
  override def mkData(partsMap: PartsMap): IO[Either[String, Data]] = for {
    // 1. Make some checks on the parameters to distinguish between Data types
    compoundData  <- partsMap.optPartValue(CompoundDataParameter.name)
    paramEndpoint <- partsMap.optPartValue(EndpointParameter.name)

    // 2. Delegate on the correct sub-class for creating the Data
    maybeData <- {
      // 1. Compound data
      if(compoundData.isDefined) DataCompound.mkData(partsMap)
      // 2. Endpoint data
      else if(paramEndpoint.isDefined) DataEndpoint.mkData(partsMap)
      // 3. Simple data or unknown
      else DataSingle.mkData(partsMap)
    }

  } yield maybeData
}

/** Static utilities to be used with [[Data]] representations
  *
  * @tparam D Specific [[Data]] representation to be handled
  */
private[data] trait DataCompanion[D <: Data] extends LazyLogging {

  /** Empty instance of the [[Data]] representation in use
    */
  val emptyData: D

  /** Encoder used to transform [[Data]] instances to JSON values
    */
  implicit val encodeData: Encoder[D]

  /** Decoder used to extract [[Data]] instances from JSON values
    */
  implicit val decodeData: Decoder[D]

  /** Given a request's parameters, try to extract an instance of [[Data]] (type [[D]]) from them
    *
    * @param partsMap Request's parameters
    * @return Either the [[Data]] instance or an error message
    */
  def mkData(partsMap: PartsMap): IO[Either[String, D]]
}
