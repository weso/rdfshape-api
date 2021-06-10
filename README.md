# RDFShape

RDFShape is web API for semantic data analysis and validation implemented in Scala using
the [http4s](https://http4s.org/) library.

This repository contains the backend part of RDFShape and acts as a queryable API to be consumed by clients. To learn
more about our public client, see its [GitHub repository](https://github.com/weso/rdfshape-client)
or [try it](https://rdfshape.weso.es).

[![Continuous Integration](https://github.com/weso/rdfshape-api/actions/workflows/ci.yml/badge.svg)](https://github.com/weso/rdfshape-api/actions/workflows/ci.yml)
[![Docker build](https://github.com/weso/rdfshape-api/actions/workflows/publish_docker.yml/badge.svg)](https://github.com/weso/rdfshape-api/actions/workflows/publish_docker.yml)

[![Codacy](https://api.codacy.com/project/badge/Grade/2ad10ec42b6a4bb389aeb114fe192f21)](https://www.codacy.com/gh/weso/rdfshape?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weso/rdfshape&amp;utm_campaign=Badge_Grade)

# Deployed versions of RDFShape API

RDFShape is already deployed [here](https://api.rdfshape.weso.es/api).

# Quick reference

- [WESO Research Group](https://weso.es)
- [Base repository](https://github.com/weso/rdfshape)
- [Validating RDF data book](http://book.validatingrdf.com)
- [Wiki](https://github.com/weso/rdfshape/wiki)
- [API Docs](https://app.swaggerhub.com/apis/weso/RDFShape/) (WIP)
- [Scaladoc](https://www.weso.es/rdfshape-api/)

# Installation and Usage

## Deploying locally

### Requirements

* RDFShape API requires [SBT](https://www.scala-sbt.org/) to be built

### Steps

1. Clone this repository
2. Go to directory where RDFShape source code is located and execute `sbt "run --help"` After some time downloading
   dependencies and compiling the source code, the application will show the help menu if everything went right.
3. From this point, execute `sbt "run --server"` for the API to launch and be accessible
   at [localhost:8080](http://localhost:8080)
4. To use a different port run `sbt "run --server --port <port-number>"`. For further information, refer to the help
   menu.

## Deploying with Docker

* Use the provided Dockerfile to build rdfshape-client or pull
  from [Github Container Registry](https://github.com/orgs/weso/packages/container/package/rdfshape-client).

### Building the image

* When building the Docker image, you may provide the following arguments via `--build-arg`:
    * **GITHUB_TOKEN** [required]:
        - A valid GitHub token to download WESO project dependencies from Github packages. This is required when
          manually building the image.
        - Images available in [GCR](https://github.com/orgs/weso/packages/container/package/rdfshape-client)
          have already been built using a read-only token for downloading the dependencies.

### Running containers

* When running a container, you may provide the following environment variables via `--env`:
    - `PORT` [optional]:
        - Port where the API is exposed inside the container. Default is 8080.
    - `USE_HTTPS` [optional]:
        - Any value to try to serve via HTTPS, undefined for HTTP.

## Serving through HTTPS

* You may want to serve RDFShape using HTTPS:
    1. Launch the application with the `--https` argument (in containers, set the )
    2. Set up your certificates. There are **two ways** to do so:
        1. **[Recommended]** Web server setup:
            - Run a web server (i.e., Nginx) in your machine or in a separate container and configure it as a reverse
              proxy that forwards incoming requests to RDFShape.
            - Launch the application **normally** (no `--https` is required).
        2. Manual setup:
            - Set the following environment variables in your server/container, so it can search and use your
              certificates in a [Java keystore](https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html):
                - `KEYSTORE_PATH`: location of the keystore storing the certificate.
                - `KEYSTORE_PASSWORD`: password protecting the keystore (leave empty if there is none).
                - `KEYMANAGER_PASSWORD`: password protecting the certificate (leave empty is there is none).
            - Launch the application with the `--https` argument (in containers, set `USE_HTTPS`).

### Supported tags

- _:stable_: Stable build updated manually.
- <_:hashed_tags_>: Automated builds by our CI pipeline. With the latest features uploaded to our repository but lacking
  internal testing.

# Help menu

The program supports the following CLI arguments:

```
rdfshape 0.1 - WESO Research Group (https://www.weso.es/)

USAGE: rdfshape --port [port number]
rdfshape is an mechanism for processing, validating and visualizing semantic data (RDF, SHEX, SHACL and more) through a REST API.

Options:
    --https         Attempt to serve the API via HTTPS (defaults to false)
-p, --port  <arg>   Port in which the API will listen for requests (defaults
                    to 8080)
-s, --server        Launch an HTTP server that will listen for request on the
                    specified port (this option can be omitted)
    --help          Show help message
    --version       Show version of this program
```

# Dependencies

RDFShape server has been implemented in Scala using the following libraries:

* [SHaclEX](https://github.com/labra/shaclex): a Scala implementation of ShEx and SHACL.
* [http4s](https://http4s.org/): a purely functional library for http.
* [cats](https://typelevel.org/cats/): a library for functional programming in Scala.
* [scallop](https://github.com/scallop/scallop): a simple command-line arguments parsing library for Scala.
* [UMLShaclex](https://github.com/labra/shaclex): contains the visualization code that converts schemas to UML diagrams.
* [SRDF](http://www.weso.es/srdf/): is the library used to handle RDF. It is a simple interface with 2 implementations,
  one in [Apache Jena](https://jena.apache.org/), and the other in [RDF4j](https://rdf4j.org/).
* [Any23](https://any23.apache.org/): is used by RDFShape to convert HTML files in RDFa and Microdata to RDF.
* [Topbraid SHACL API](https://github.com/TopQuadrant/shacl): is used to add another SHACL engine apart of the SHaclEX
  and Apache Jena SHACL engines.

# Contribution and issues

We really appreciate contributions. Please fork this repository and open a pull request to add more features or submit
issues:

* [Issues about RDFShape API](https://github.com/weso/rdfshape-api/issues)
* [Issues about RDFShape client](https://github.com/weso/rdfshape-client/issues)
* [Issues about SHACLex validation library](https://github.com/labra/shaclex/issues)

<a href="https://github.com/weso/rdfshape/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=weso/rdfshape"  alt="RdfShape contributors"/>
</a>
