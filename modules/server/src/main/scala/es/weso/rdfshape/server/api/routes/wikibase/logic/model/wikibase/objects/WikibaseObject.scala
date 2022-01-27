package es.weso.rdfshape.server.api.routes.wikibase.logic.model.wikibase.objects

import org.http4s.Uri

/** General class representing any object (entity, schema...) living in a
  * wikibase instance
  *
  * @param entityUri URL where the object data can be found
  */
private[api] class WikibaseObject(val entityUri: Uri)
