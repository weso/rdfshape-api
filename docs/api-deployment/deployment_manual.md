---
id: deployment_manual
title: Running with SBT
---

# Running with SBT

## Requirements

* @APP_NAME@ requires [SBT](https://www.scala-sbt.org/) to be built

## Interactive mode with SBT

The way to familiarize yourself with the software is to run the `sbt` command, which will open the _sbt shell_,
and execute commands from there.

1. Clone this repository
2. Go to directory where @APP_NAME@ source code is located and execute `sbt`. After some time downloading dependencies and
   compiling the source code, the _sbt shell_ will launch if everything went right.
3. From this point, you may execute several commands from the sbt shell:
    - `run` for the API to launch and be accessible at [localhost:8080](http://localhost:8080).
    - `run --help` to see the help menu with further usage
      information.

## Binary mode

The fastest way to run @APP_NAME@ is to compile the code and generate an executable file:

1. Clone this repo and run `sbt`, as seen above.
2. From the sbt shell, run `Universal/packageBin`.
3. A zip file with an executable and all the program dependencies will be created
   inside `(ProjectFolder)/target/universal`.

## Serving with HTTPS

You can serve @APP_NAME@ with HTTPS in 2 ways:

1. **[Recommended]** Web server setup:
    - Run a web server (i.e., Nginx) in your machine or in a separate container and configure it as a reverse proxy that
      forwards incoming requests to the API. Configure your web server to use HTTPS to communicate with clients.
    - Launch the application **normally** (no `--https` is required, the web server will handle it).
2. Manual setup:
    - Set the following environment variables in your machine/container, so it can search and use your certificates in
      a [Java keystore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html):
        - `KEYSTORE_PATH`: location of the keystore storing the certificate.
        - `KEYSTORE_PASSWORD`: password protecting the keystore (leave empty if there is none).
        - `KEYMANAGER_PASSWORD`: password protecting the certificate (leave empty is there is none).
    - Launch the application with the `--https` argument.
