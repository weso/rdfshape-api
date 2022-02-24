package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.NONE
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.{Compact, Turtle}
import es.weso.rdfshape.server.api.routes.data.logic.DataSource
import es.weso.rdfshape.server.api.routes.data.logic.types.DataSingle
import es.weso.rdfshape.server.api.routes.data.logic.types.merged.DataCompound
import es.weso.rdfshape.server.api.routes.schema.logic.operations.SchemaValidate
import es.weso.rdfshape.server.api.routes.schema.logic.trigger.TriggerShapeMap
import es.weso.rdfshape.server.api.routes.schema.logic.types.Schema
import es.weso.rdfshape.server.api.routes.shapemap.logic.{
  ShapeMap,
  ShapeMapSource
}
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikidata.WikidataEntity
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperation.wikidataOnlyMessage
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import es.weso.shapemaps.{Start, ShapeMap => ShapeMapW}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax

import scala.util.{Failure, Success, Try}

/** Given an input [[WikibaseOperationDetails]] and a Schema, attempt to Validate entities
  * in a wikibase using wikidata schemas or shape expressions
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @param schema        [[Schema]] against which to validate entities
  * @note Only available for Wikidata
  */
private[wikibase] case class WikibaseSchemaValidate(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO],
    schema: Schema
) extends WikibaseOperation(
      WikibaseSchemaValidate.successMessage,
      operationData,
      client
    )
    with LazyLogging {

  override lazy val targetUri: Uri = uri"" // unused

  private val entitiesSeparator = '|'

  override def performOperation: IO[WikibaseOperationResult] = {

    val entityUris = operationData.payload.split(entitiesSeparator)

    // Raise error if target is not Wikidata
    if(targetWikibase != Wikidata)
      IO.raiseError(WikibaseServiceException(wikidataOnlyMessage))
    else {
      val tryResult = for {
        // Get the Wikidata items info from the URI submitted as payload
        wdEntities <- Try {
          entityUris.map(it => WikidataEntity(Uri.unsafeFromString(it)))
        }
        // Create the data to be validated, using Wikidata to get the URL
        // to the Turtle contents
        /* Data will be a compound of several simple data fetched from each
         * entity */

        inputData = DataCompound(
          wdEntities
            .map(entity =>
              DataSingle(
                dataPre = Some(entity.contentUri.renderString),
                dataFormat = Turtle,
                inference = NONE,
                dataSource = DataSource.URL
              )
            )
            .toList
        )

        /* Get the schema model needed for validation: already passed to the
         * class */

        // Perform validation
        eitherValidationResults = for {
          // Create your trigger mode: ShEx with basic Shapemap
          // For each entity, add a start point
          shapeMapModel <- wdEntities.foldLeft(ShapeMapW.empty.asRight[String])(
            (sm, entity) => {
              val entityUri = IRI(entity.entityUri.renderString)
              sm.flatMap(_.add(entityUri, Start))
            }
          )
          shapeMapFinalModel <- shapeMapModel.serialize(Compact.name)
          trigger = TriggerShapeMap(
            shapeMap = ShapeMap(
              content = shapeMapFinalModel,
              nodesPrefixMap = shapeMapModel.nodesPrefixMap.addPrefixMap(
                Wikidata.wikidataPrefixMap
              ),
              shapesPrefixMap = shapeMapModel.shapesPrefixMap.addPrefixMap(
                Wikidata.wikidataPrefixMap
              ),
              format = Compact,
              source = ShapeMapSource.TEXT
            ),
            data = Some(inputData),
            schema = Some(schema)
          )
          result = SchemaValidate.schemaValidate(
            inputData,
            schema,
            trigger
          )
        } yield result
      } yield eitherValidationResults match {
        case Left(err)    => IO.raiseError(WikibaseServiceException(err))
        case Right(value) => value
      }

      // Return an error or the contained value wrapped into a Result object
      tryResult match {
        case Failure(exception) =>
          IO.raiseError(WikibaseServiceException(exception.getMessage))
        case Success(value) =>
          value.map(validationResults =>
            WikibaseOperationResult(
              operationData = operationData,
              wikibase = targetWikibase,
              result = Json.fromFields(
                List(
                  ("entity", entityUris.asJson),
                  ("result", validationResults.asJson)
                )
              )
            )
          )
      }
    }
  }
}

object WikibaseSchemaValidate {
  private val successMessage = "Schema contents validated successfully"
}
