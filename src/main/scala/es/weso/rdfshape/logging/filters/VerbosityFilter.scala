package es.weso.rdfshape.logging.filters

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import es.weso.rdfshape.logging.LoggingManager.systemPropertyVerbosity

/**  Decide whether a logging event should be performed or not based on
  * the user-selected verbosity when run through the CLI
  */
class VerbosityFilter extends Filter[ILoggingEvent] {

  /** Represents the user selected verbosity of the app, used to filter
    * console log output. It is retrieved from a custom System property.
    * @note Use lazy to delay the property computation until it is set and needed
    */
  lazy val userVerbosity: Level =
    Level.toLevel(System.getProperty(systemPropertyVerbosity), Level.ERROR)

  /** Decide whether a logging event should be performed or not based on user preferences
    * @param event Input logging event
    * @return Accept the logging request if it does comply with the user verbosity level,
    *         else deny
    */
  override def decide(event: ILoggingEvent): FilterReply =
    userVerbosity match {
      case Level.OFF => FilterReply.DENY
      case _ =>
        if(event.getLevel.isGreaterOrEqual(userVerbosity)) FilterReply.ACCEPT
        else FilterReply.DENY
    }
}
