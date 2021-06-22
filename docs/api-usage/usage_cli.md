---
id: usage_cli
title: Command Line Interface
---

# Command Line Interface

## Command Reference

@APP_NAME@'s CLI currently supports the following launch-arguments:
- _--https_ Attempt to serve the API via HTTPS (defaults to false), searching for certificates as specified in the current environment.
- _-p, --port_  Port in which the API will listen for requests. Values must be in range 1-65535 (defaults to 8080).
- _--verbose_ Print some additional data as it is processed by the server (defaults to false)
- _--version_ Print the version of the program
- _--help_ Print the help menu

## Examples
1. Launching @APP_NAME@ in port 8081:
- `rdfshape -p 8081`
2. Launching @APP_NAME@ in port 80, try to use the HTTPS configuration from the environment:
- `rdfshape -p 80 --https`
