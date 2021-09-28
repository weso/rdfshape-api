package es.weso.rdfshape.server.api.routes.data.logic.data

import cats.effect.{IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFReasoner
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.routes.data.logic.data.DataSource.DataSource
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  CompoundDataParameter,
  EndpointParameter
}
import es.weso.rdfshape.server.api.utils.parameters.PartsMap
import io.circe.{Decoder, Encoder, HCursor, Json}

/** Common trait to all data, whichever its nature (single, compound, endpoint...)
  */
trait Data {

  /** Source where the data comes from
    */
  val dataSource: DataSource

  /** Given an RDF source of data, try to parse it and get the RDF model representation
    *
    * @return RDF logical model of the data contained
    */
  def toRdf(relativeBase: Option[IRI] = None): IO[Resource[IO, RDFReasoner]]
}

object Data extends DataCompanion[Data] {

  /** Dummy implementation meant to be overridden
    */
  override val emptyData: Data = SimpleData.emptyData

  /** Dummy implementation meant to be overridden
    */
  override implicit val encodeData: Encoder[Data] = _ => Json.fromString("")

  /** Dummy implementation meant to be overridden
    */
  override implicit val decodeData: Decoder[Data] = (_: HCursor) =>
    Right(emptyData)

  /** General implementation delegating on subclasses
    */
  override def mkData(partsMap: PartsMap): IO[Either[String, Data]] = for {
    compoundData  <- partsMap.optPartValue(CompoundDataParameter.name)
    paramEndpoint <- partsMap.optPartValue(EndpointParameter.name)

    maybeData <- {
      // Create one of: Simple Data, Compound Data or Endpoint Data
      // 1. Compound data
      if(compoundData.isDefined) CompoundData.mkData(partsMap)
      // 2. Endpoint data
      else if(paramEndpoint.isDefined) EndpointData.mkData(partsMap)
      // 3. Simple data or unknown
      else SimpleData.mkData(partsMap)
    }

  } yield maybeData
}

/** Static utilities to be used with Data representations
  *
  * @tparam D Specific data representation to be handled
  */
trait DataCompanion[D <: Data] extends LazyLogging {

  /** Empty instance of the data representation in use
    */
  val emptyData: D

  /** Encoder used to transform Data instances to JSON values
    */
  implicit val encodeData: Encoder[D]

  /** Decoder used to extract Data instances from JSON values
    */
  implicit val decodeData: Decoder[D]

  /** Given a request's parameters, try to extract an instance of Data (type D) from them
    *
    * @param partsMap Request's parameters
    * @return Either the Data instance or an error message
    */
  def mkData(partsMap: PartsMap): IO[Either[String, D]]
}
