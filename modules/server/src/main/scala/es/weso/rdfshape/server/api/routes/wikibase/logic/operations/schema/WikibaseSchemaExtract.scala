package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.schema

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.rdfshape.server.api.format.dataFormats.Turtle
import es.weso.rdfshape.server.api.format.dataFormats.schemaFormats.ShExC
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.Wikidata
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikibase.WikibaseEntity
import es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects.wikidata.WikidataEntity
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.{
  WikibaseOperation,
  WikibaseOperationDetails,
  WikibaseOperationResult
}
import es.weso.rdfshape.server.utils.error.exceptions.WikibaseServiceException
import es.weso.schema.{Schemas, Schema => SchemaW}
import es.weso.schemaInfer.{InferOptions, SchemaInfer}
import es.weso.shapemaps.{RDFNodeSelector, ResultShapeMap}
import es.weso.utils.IOUtils.{either2es, io2es}
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax

import scala.util.{Failure, Success, Try}

/** Given an input [[WikibaseOperationDetails]], attempt to extract an schema (ShEx)
  * from a given entity present in the target wikibase instance
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note Only available for Wikidata
  * @note Should be passed a client with redirect
  */
private[wikibase] case class WikibaseSchemaExtract(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseOperation(
      WikibaseSchemaExtract.successMessage,
      operationData,
      client
    )
    with LazyLogging {

  override lazy val targetUri: Uri = uri"" // unused

  /* Legacy code that I just refactored here: https://imgur.com/gallery/DH6vrFm */
  override def performOperation: IO[WikibaseOperationResult] = {
    val entityUri = operationData.payload

    // Try to extract even from wikibase instances different from wikidata.
    val tryResult = for {
      // Get the Wikidata item info from the URI submitted as payload
      wdEntity <- Try {
        val schemaUri = Uri.unsafeFromString(entityUri)
        // Build the target wikibase, as sent by the user
        if(targetWikibase == Wikidata) WikidataEntity(schemaUri)
        else new WikibaseEntity(targetWikibase, schemaUri)
      }
      eitherResult =
        for {
          // Raw RDF of the Wikidata entity as String
          strRdf <- io2es(client.expect[String](wdEntity.contentUri))

          // Infer schema magic
          eitherInferred <- io2es(
            RDFAsJenaModel
              .fromString(strRdf, Turtle.name)
              .flatMap(
                _.use(rdf =>
                  SchemaInfer.runInferSchema(
                    rdf,
                    RDFNodeSelector(IRI(entityUri)),
                    Schemas.shEx.name,
                    IRI(s"http://example.org/Shape_${wdEntity.localName}"),
                    InferOptions.defaultOptions.copy(maxFollowOn = 3)
                  )
                )
              )
          )
          // Tuple with the infer results
          pair <- either2es[(SchemaW, ResultShapeMap)](eitherInferred)

          // Form the schema in "ShExC format that will be part of the result
          shExCStr <- io2es({
            val (schema, _) = pair
            schema.serialize(ShExC.name.toUpperCase)
          })

        } yield WikibaseOperationResult(
          operationData = operationData,
          wikibase = targetWikibase,
          result = Json.fromFields(
            List(
              ("entity", Json.fromString(entityUri)),
              ("schema", Json.fromString(shExCStr))
            )
          )
        )

    } yield eitherResult.value.flatMap {
      case Left(err)    => IO.raiseError(new WikibaseServiceException(err))
      case Right(value) => IO.pure(value)
    }

    // Return an error or the contained value
    tryResult match {
      case Failure(exception) =>
        IO.raiseError(new WikibaseServiceException(exception))
      case Success(value) => value
    }

  }
}

object WikibaseSchemaExtract {
  private val successMessage = "Schema contents extracted successfully"
}
