package es.weso.rdfshape.server.api.routes.schema.service.operations.stream.configuration

import cats.implicits.toBifunctorOps
import es.weso.rdfshape.server.api.utils.parameters.IncomingRequestParameters.{
  GroupIdParameter,
  PortParameter,
  ServerParameter,
  TopicParameter
}
import io.circe.{Decoder, HCursor}
import org.ragna.comet.stream.extractors.kafka.{
  KafkaExtractor,
  KafkaExtractorConfiguration
}

import scala.util.Try

/** Minor configuration class, containing the information required for a
  * Comet Kafka extractor to fetch an incoming stream
  *
  * Everything but the server and topic is optional or has a default value
  *
  * @see [[KafkaExtractor]]
  * @see [[KafkaExtractorConfiguration]]
  */
final case class StreamValidationStreamConfiguration(
    server: String,
    port: Option[Int],
    topic: String,
    groupId: Option[String]
) {
  // Pre-requisites: None
}

object StreamValidationStreamConfiguration {

  /** Custom decoder fetching the correct parameter names in the incoming
    * JSON documents
    */
  implicit val decoder
      : Decoder[Either[String, StreamValidationStreamConfiguration]] =
    (cursor: HCursor) =>
      for {
        server <- cursor
          .downField(ServerParameter.name)
          .as[String]

        optPort <- cursor
          .downField(PortParameter.name)
          .as[Option[Int]]

        topic <- cursor
          .downField(TopicParameter.name)
          .as[String]

        groupId <- cursor
          .downField(GroupIdParameter.name)
          .as[Option[String]]
      } yield {
        Try {
          StreamValidationStreamConfiguration(server, optPort, topic, groupId)
        }.toEither.leftMap(err =>
          s"Could not build the stream configuration from user data:\n ${err.getMessage}"
        )
      }
}
