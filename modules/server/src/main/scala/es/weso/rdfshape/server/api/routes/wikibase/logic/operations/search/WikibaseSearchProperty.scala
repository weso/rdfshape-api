package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.search

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationDetails
import org.http4s.client.Client

/** A [[WikibaseSearchOperation]] searching for properties in a wikibase instance.
  * Resorts to [[https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities]]
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  */
sealed case class WikibaseSearchProperty private (
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseSearchOperation(
      operationData,
      client,
      WikibaseSearchTypes.PROPERTY
    )
