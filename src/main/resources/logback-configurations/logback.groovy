import ch.qos.logback.classic.filter.ThresholdFilter

// https://logback.qos.ch/manual/groovy.html
statusListener(NopStatusListener) // Prevent logback debug messages on console

def LOGS_FOLDER = "logs"
def systemPropertyVerbosity = "rdfshape.api.verbosity.level"

/* Define log appenders for Console, Files, etc. */

// Rolling file appender. Create several files inside "logs" folder. Archive and compress old logs.
// Store 3 months of logs before rollback (http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy)
appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        Pattern = "%d %level %thread %mdc %logger - %m%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "$LOGS_FOLDER/%d{yyyy/MM}/%d{yyyy-MM-dd, aux}.log"
        maxHistory = 3
    }
}

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
// Root logger:
//  - Show console messages (adapt ThresholdFilter to the user-selected verbosity level with system properties)
//  - Append DEBUG and higher messages to the log files, whether they are shown on console or not
root(DEBUG, ["CONSOLE", "ROLLING"])

/*Additional settings */
scan("30 seconds")