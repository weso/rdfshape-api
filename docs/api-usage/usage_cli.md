---
id: usage_cli
title: Command Line Interface
---

# Command Line Interface

## Command Reference

@APP_NAME@'s CLI currently supports the following launch-arguments:

- `--https` Attempt to serve the API via HTTPS (default is false), searching for
  certificates as specified in the current environment.
- `-p, --port <arg>`  Port in which the API will listen for requests. Values
  must be in range 1-65535 (default is 8080).
- `-s, --silent`  Enable silent mode in order not to log any output to console (
  default is false).
- `-t, --stream-timeout  <arg>`  Seconds that the server will wait before
  closing a streaming validation for which no data is received. Values must be
  in range 1-1800 (default is 40).
- `-v, --verbose` Show additional logging information (use cumulative times for
  additional info, like: `-vvv`).
- `--version` Print the version of the program.
- `--help` Print the help menu.

## Verbosity levels

When using the `-v, --verbose` CLI argument, the following logging messages are
shown on console at each time:

- `No verbose argument` **ERROR** level messages
- `-v` **WARN** level messages and upwards
- `-vv` **INFO** level messages and upwards (includes client connections and
  requests)
- `-vvv` **DEBUG** level messages and upwards

## JVM Custom Arguments

In case @APP_NAME@ is having trouble to generate permalinks due to an SSL issue,
try adding the following argument:

- `-Djdk.tls.client.protocols=TLSv1.2`

## Examples

1. Launching @APP_NAME@ in port 8081:

- `rdfshape -p 8081`

2. Launching @APP_NAME@ in port 80, try to use the HTTPS configuration from the
   environment:

- `rdfshape -p 80 --https`

3. Launching @APP_NAME@ in port 8080, with the maximum verbosity level:

- `rdfshape -vvv`
