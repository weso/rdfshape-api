// https://logback.qos.ch/manual/groovy.html
statusListener(NopStatusListener) // Prevent logback debug messages on console

def LOGS_FOLDER = "logs"

/* Define log appenders for Console, Files, etc. */

// Rolling file appender. Create several files inside "logs" folder. Archive and compress old logs.
// Store 6 months of logs (http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy)
appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        Pattern = "%d %level %thread %mdc %logger - %m%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "$LOGS_FOLDER/rdfshape_api-%d{yyyy-MM}.zip"
        maxHistory = 6
    }
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}


/* Define logging levels and associated appenders */
root(WARN, ["ROLLING", "CONSOLE"])

/*Additional settings */
