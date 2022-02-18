package es.weso.rdfshape.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.util.ContextInitializer

/** Utilities related to the logging system used by the application to log information on the console and the logs folder
  * @see {@link es.weso.rdfshape.logging.LoggingManager}
  */
final class LoggingManager

object LoggingManager {

  /** System property checked by logback to know how much console-output it should filter
    */
  val systemPropertyVerbosity = "rdfshape.api.verbosity.level"

  /** Location of logback's configuration file inside the "resources" folder
    * @note Resorts to [[ContextInitializer.AUTOCONFIG_FILE]], i.e.: logback.xml
    */
  private val logbackConfigurationFile =
    ContextInitializer.AUTOCONFIG_FILE

  /** Set the System Properties that will be read in logback's configuration file to define logback's behavior
    * @see setUpLogbackConfiguration
    * @see setUpLogbackLogLevel
    */
  def setUp(
      verbosity: Int,
      silent: Boolean,
      logbackConfigurationFile: String = logbackConfigurationFile
  ): Unit = {
    setUpLogbackLogLevel(verbosity, silent)
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
  private def setUpLogbackLogLevel(verbosity: Int, silent: Boolean): Unit = {
    val logLevel =
      if(silent) Level.OFF else mapVerbosityValueToLogLevel(verbosity)
    System.setProperty(
      systemPropertyVerbosity,
      logLevel.toString
    )
  }

  /** Given a verbosity numeric value, map it to its corresponding logging level
    * @param verbosity Verbosity numeric value
    * @return A string representing the minimum level of the logs to be shown on console
    */
  def mapVerbosityValueToLogLevel(verbosity: Int): Level = {
    verbosity match {
      case 0 => Level.ERROR // No verbose argument. Show errors.
      case 1 => Level.WARN  // -v. Show warnings.
      case 2 => Level.INFO  // -vv. Show info.
      case _ => Level.DEBUG // -vvv and forth. Show debug information.
    }
  }
}
