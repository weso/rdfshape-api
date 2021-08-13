package es.weso.rdfshape.server.api.merged

import com.typesafe.scalalogging.LazyLogging

/** Abstract representation of the active tab in the client that sent the request.
  * Used to distinguish whether the submitted text, URL or file should be prioritized in case several are present.
  */
sealed abstract class ActiveDataTab {

  /** Unique identifier of the active tab
    */
  val id: String
}

/** Indicates the client uploaded RDF data as raw text
  */
case object DataTextArea extends ActiveDataTab {
  override val id = "#dataTextArea"
}

/** Indicates the client uploaded RDF data by indicating the URL where it lives
  */
case object DataUrl extends ActiveDataTab {
  override val id = "#dataUrl"
}

case object DataEndpoint extends ActiveDataTab {
  override val id = "#dataEndpoint"
}

/** Indicates the client uploaded RDF data by uploading a file containing it
  */
case object DataFile extends ActiveDataTab {
  override val id = "#dataFile"
}

object ActiveDataTab extends LazyLogging {

  /** Default value to use if none is present
    */
  lazy val default: ActiveDataTab = dataTabValues.head

  /** All possible values the DataTab may acquire
    */
  private val dataTabValues =
    List(DataTextArea, DataUrl, DataFile, DataEndpoint)

  /** Given a tab identifier (name), find and returning the corresponding data tab with that id
    *
    * @param tabId Id of the Tab to be returned
    * @return The corresponding ActiveDataTab if the tabName exists, otherwise an error message
    */
  def fromString(tabId: String): Either[String, ActiveDataTab] = {

    dataTabValues.collectFirst {
      case value if value.id == tabId => value
    } match {
      case Some(v) => Right(v)
      case None =>
        val errorMsg = mkErrorMessage(tabId)
        logger.error(errorMsg)
        Left(errorMsg)
    }
  }

  private def mkErrorMessage(id: String): String = {
    s"Unknown value for activeDataTab: $id. Available values: ${dataTabValues.map(_.id).mkString(",")}"
  }
}
