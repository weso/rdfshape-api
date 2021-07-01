package es.weso.rdfshape.logging

import ch.qos.logback.classic.util.ContextInitializer

/** Utilities related to the logging system used by the application to log information on the console and the logs folder
  * @see {@link es.weso.rdfshape.logging.LoggingManager}
  */
final class LoggingManager

object LoggingManager {

  /** System property checked by logback to know how much console-output it should filter
    */
  private val systemPropertyVerbosity = "rdfshape.api.verbosity.level"

  /** Default location if logback's configuration file inside the "resources" folder
    */
  private val defaultLogbackConfigurationFile =
    "logback-configurations/logback.groovy"

  /** Given a verbosity numeric value, map it to its corresponding logging level
    * @param verbosity Verbosity numeric value
    * @return A string representing the minimum level of the logs to be shown on console
    */
  def mapVerbosityValueToLogLevel(verbosity: Int): String = {
    verbosity match {
      case 0 => LoggingLevel.ERROR // No verbose argument. Show errors.
      case 1 => LoggingLevel.WARN  // -v. Show warnings.
      case 2 => LoggingLevel.INFO  // -vv. Show info.
      case _ => LoggingLevel.DEBUG // -vvv and forth. Show debug information.
    }
  }

  /** Set the System Properties that will be read in logback's configuration file to define logback's behavior.
    * @see setUpLogbackConfiguration
    * @see setUpLogbackLogLevel
    */
  def setUp(
      verbosity: Int,
      logbackConfigurationFile: String = defaultLogbackConfigurationFile
  ): Unit = {
    setUpLogbackLogLevel(verbosity)
    setUpLogbackConfiguration(logbackConfigurationFile)
  }

  /** Set the system property which defines the configuration file that logback uses.
    * @param configurationFile filename of the logback configuration file (relative to the "resources" folder)
    */
  private def setUpLogbackConfiguration(configurationFile: String): Unit = {
    System.setProperty(
      ContextInitializer.CONFIG_FILE_PROPERTY,
      configurationFile
    )
  }

  /** Set the system property which defines the amount of logs that will appear
    * in console (see logback configuration file).
    * @param verbosity numeric representation of the required verbosity level
    */
  private def setUpLogbackLogLevel(verbosity: Int): Unit = {
    System.setProperty(
      systemPropertyVerbosity,
      mapVerbosityValueToLogLevel(verbosity)
    )
  }
}

/** Enum classifying the accepted logging levels by their String representation.
  */
object LoggingLevel extends Enumeration {
  type LoggingLevel = String
  val ERROR = "ERROR"
  val WARN  = "WARN"
  val INFO  = "INFO"
  val DEBUG = "DEBUG"
  val TRACE = "TRACE"
}
