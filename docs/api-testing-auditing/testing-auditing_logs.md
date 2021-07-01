---
id: testing-auditing_logs
title: Logging system
---

# Logging system

## Infrastructure

This project uses a logging framework provided by two mature libraries:

1. [Logback](http://logback.qos.ch/): Framework's back-end, provides customizable logging levels and log appenders for
   console, files, etc.

2. [scala-logging](https://github.com/lightbend/scala-logging): Framework's front-end, reduces the verbosity of logging
   messages from the code thanks to several macros and utilities.

## Functionality

### Console log messages

@APP_NAME@ is configured to use a [Console Appender](http://logback.qos.ch/manual/appenders.html#ConsoleAppender) to log
messages to the console, refer to [CLI section](/rdfshape-api/docs/api-usage/usage_cli) to configure what is logged to
the console via CLI arguments.

### File log messages

@APP_NAME@ is configured to use
a [Rolling File Appender](http://logback.qos.ch/manual/appenders.html#RollingFileAppender) to store all log messages of
level **DEBUG** and above inside _.log_ files, whether this messages are verbosely shown on console or not.

The logs written to the files:

- Are located inside a `logs` folder, in the application's execution path. Therefore, make sure you run the app with a
  user with write access and from a location that is writable.

- Follow a [time-based rolling policy](http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy), which
  implies that logs are rotated and compressed in a periodic basis defined in logback's configuration file.

### Adding custom functionality

The project is already configured to work as explained above. For further configuration, check
the [logback.groovy](https://github.com/weso/rdfshape-api/blob/master/src/main/resources/logback-configurations/logback.groovy)
configuration file or the documentation of each respective library.