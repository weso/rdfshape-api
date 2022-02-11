package es.weso.rdfshape.server.api.routes.wikibase.logic.operations.get

import cats.effect.IO
import es.weso.rdfshape.server.api.routes.wikibase.logic.operations.WikibaseOperationDetails
import org.http4s.client.Client

/** A [[WikibaseGetOperation]] returning the labels of the objects retrieved.
  *
  * @param operationData Data needed to perform the wikibase operation
  * @param client        [[Client]] object to be used in requests to wikibase
  * @note All derived operations are based on [[https://www.wikidata.org/w/api.php?action=help&modules=wbgetentities]]
  */
sealed case class WikibaseGetLabels(
    override val operationData: WikibaseOperationDetails,
    override val client: Client[IO]
) extends WikibaseGetOperation(
      operationData,
      client,
      List(WikibasePropTypes.LABELS)
    )
