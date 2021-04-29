# RDFShape

RDFShape is web API for semantic data analysis and validation implemented in Scala using the [http4s](https://http4s.org/) library.

This repository contains the backend part of RDFShape and acts as a queryable API to be consumed by clients. To learn more about our public client, see its [GitHub repository](https://github.com/weso/rdfshape-client) or [try it](https://rdfshape.weso.es). 


[![Continuous Integration](https://github.com/weso/rdfshape/actions/workflows/ci.yml/badge.svg)](https://github.com/weso/rdfshape/actions/workflows/ci.yml)
[![Docker build](https://github.com/weso/rdfshape/actions/workflows/publish_docker.yml/badge.svg)](https://github.com/weso/rdfshape/actions/workflows/publish_docker.yml)

[![Codacy](https://api.codacy.com/project/badge/Grade/2ad10ec42b6a4bb389aeb114fe192f21)](https://www.codacy.com/gh/weso/rdfshape?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weso/rdfshape&amp;utm_campaign=Badge_Grade)

# Deployed versions of RDFShape API

RDFShape is already deployed [here](http://rdfshape.weso.es:8080).

# Quick reference

- Maintained by: [WESO Research Group](https://weso.es)
- Where to help/learn more: [base repository](https://github.com/weso/rdfshape)
- Background information about validating RDF: [Validating RDF data book](http://book.validatingrdf.com)
- Project [wiki](https://github.com/weso/rdfshape/wiki)


# Installation

## Deploy locally

### Requirements

* RDFShape API requires [SBT](https://www.scala-sbt.org/) to be built

### Steps
1. Clone this repository
2. Go to directory where RDFShape source code is located and execute `sbt run`
3. After some time downloading and compiling uri (the source code it will start the application, which can be accessed at [localhost:8080](http://localhost:8080)
4. To use a different port run `sbt "run --server --port <PortNumber>"`

## Deploy with Docker

* Use the provided Dockerfile to build rdfshape-client or pull from [Github Container Registry](https://github.com/orgs/weso/packages/container/package/rdfshape-client).

### Building the image

* When building the Docker image, you may provide the following arguments
  via `--build-arg`:
    * **GITHUB_TOKEN** [required]:
        - A valid GitHub token to download WESO project dependencies from Github
          packages. This is required when manually building the image.
        - Images available
          in [Docker Hub](https://hub.docker.com/r/wesogroup/rdfshape-api) have
          already been built using a read-only token for downloading the
          dependencies.

### Running containers
          
* When running a container, you may provide the following environment variables
  via `--env`:
    - **PORT** [optional]:
       - Port where the API is exposed inside the container. Default is 80.

### Supported tags
- _:stable_: Stable build updated manually.
- <_:hashed_tags_>: Automated builds by our CI pipeline. With the latest features uploaded to our repository but lacking internal testing.

# Dependencies

RDFShape server has been implemented in Scala using the following libraries:

* [SHaclEX](https://github.com/labra/shaclex): a Scala implementation of ShEx
  and SHACL.
* [http4s](https://http4s.org/): a purely functional library for http.
* [cats](https://typelevel.org/cats/): a library for functional programming in
  Scala.
* [UMLShaclex](https://github.com/labra/shaclex): contains the visualization
  code that converts schemas to UML diagrams
* [SRDF](http://www.weso.es/srdf/): is the library used to handle RDF. It is a
  simple interface with 2 implementations, one
  in [Apache Jena](https://jena.apache.org/), and the other
  in [RDF4j](https://rdf4j.org/).
* [Any23](https://any23.apache.org/): is used by RDFShape to convert HTML files
  in RDFa and Microdata to RDF.
* [Topbraid SHACL API](https://github.com/TopQuadrant/shacl): is used to add
  another SHACL engine apart of the SHaclEX and Apache Jena SHACL engines.

# Contribution and issues

Contributions are greatly appreciated. Please fork this repository and open a
pull request to add more features or submit issues:

* [Issues about RDFShape online demo](https://github.com/labra/rdfshape/issues)
* [Issues about SHACLex validation library](https://github.com/labra/shaclex/issues)

<a href="https://github.com/weso/rdfshape/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=weso/rdfshape"  alt="RdfShape contributors"/>
</a>
