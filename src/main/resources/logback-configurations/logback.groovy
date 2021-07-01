/**
 * Logback configuration file.
 * Further documentation: https://logback.qos.ch/manual/groovy.html
 */


import ch.qos.logback.classic.filter.ThresholdFilter

/**
 * Prevent logback debug messages from being printed on console
 */
statusListener(NopStatusListener)

/**
 * Folder, relative to the program's execution path, where program logs will be stored
 */
def LOGS_FOLDER = "logs"
/**
 * Name (key) of the system property determining the application verbosity
 */
def systemPropertyVerbosity = "rdfshape.api.verbosity.level"

/* Define log appenders for Console, Files, etc. */

/**
 * Rolling file appender. Create several files inside LOGS_FOLDER. Archives and compresses old logs.
 * Store 3 months of logs before rollback (http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy)
 */
appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        Pattern = "%d %level %thread %mdc %logger - %m%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "$LOGS_FOLDER/%d{yyyy/MM}/%d{yyyy-MM-dd, aux}.log"
        maxHistory = 3
    }
}

/**
 * Console appender. Show log messages on console while the app is running.
 * Log messages are emitted from the code with the aid of the scala-logging library.
 * A filter is used to determine which messages are shown on console or not, adapting to the user's selected verbosity level.
 * http://logback.qos.ch/manual/filters.html
 */
appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
    filter(ThresholdFilter) {
        switch (System.getProperty(systemPropertyVerbosity)) {
            case "ERROR":
                level = ERROR
                break
            case "WARN":
                level = WARN
                break
            case "INFO":
                level = INFO
                break
            case "DEBUG":
                level = DEBUG
                break
            case "TRACE":
                level = TRACE
                break
            default:
                level = ERROR
                break
        }
    }
}

/* Define loggers and associated appenders */

/**
 * Root logger:
 * - Show console messages
 * - Append DEBUG and higher messages to the log files, whether they are shown on console or not
 */
root(DEBUG, ["CONSOLE", "ROLLING"])

/*Additional settings */

/**
 * Scan for configuration changes in this file periodically
 */
scan("30 seconds")