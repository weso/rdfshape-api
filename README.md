# RDFShape

RDFShape is web API for semantic data analysis and validation implemented in Scala using
the [http4s](https://http4s.org/) library.

This repository contains the backend part of RDFShape and acts as a queryable API to be consumed by clients. To learn
more about our public client, see its [GitHub repository](https://github.com/weso/rdfshape-client)
or [try it](https://rdfshape.weso.es).

[![Continuous Integration](https://github.com/weso/rdfshape-api/actions/workflows/ci.yml/badge.svg)](https://github.com/weso/rdfshape-api/actions/workflows/ci.yml)
[![Docker build](https://github.com/weso/rdfshape-api/actions/workflows/publish_docker.yml/badge.svg)](https://github.com/weso/rdfshape-api/actions/workflows/publish_docker.yml)

[![Codacy](https://api.codacy.com/project/badge/Grade/2ad10ec42b6a4bb389aeb114fe192f21)](https://www.codacy.com/gh/weso/rdfshape-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weso/rdfshape&amp;utm_campaign=Badge_Grade)

# Deployed versions of RDFShape API

RDFShape is already deployed [here](https://api.rdfshape.weso.es/api).

# Quick reference

- [Base repository](https://github.com/weso/rdfshape)
- [Wiki](https://github.com/weso/rdfshape-api/wiki)
- [Webpage](https://www.weso.es/rdfshape-api/)

---

# Deployment and Usage

Please, refer to this project's [wiki](https://github.com/weso/rdfshape-api/wiki)
or [webpage](https://www.weso.es/rdfshape-api/) for detailed information related to:

- Quickstarting the API (via SBT / Docker)
- Using the API with simple examples
- Further documentation and resources

# Publishing to OSS-Sonatype

This project uses the [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) plugin for publishing
to [OSS Sonatype](https://oss.sonatype.org/).

### SNAPSHOT Releases

Open a PR and merge it to watch the CI release a `-SNAPSHOT` version

### Full Library Releases

1. Push a tag and watch the CI do a regular release
2. `git tag -a v0.1.0 -m "v0.1.0"`
3. `git push origin v0.1.0`
   _Note that the tag version MUST start with v._

# Dependencies

RDFShape server has been implemented in Scala using the following libraries:

- [SHaclEX](https://github.com/labra/shaclex): a Scala implementation of ShEx and SHACL.
- [http4s](https://http4s.org/): a purely functional library for http.
- [cats](https://typelevel.org/cats/): a library for functional programming in Scala.
- [Logback](http://logback.qos.ch/) with [Scala Logging](https://github.com/lightbend/scala-logging): logging framework.
- [scallop](https://github.com/scallop/scallop): a simple command-line arguments parsing library for Scala.
- [UMLShaclex](https://github.com/labra/shaclex): contains the visualization code that converts schemas to UML diagrams.
- [SRDF](http://www.weso.es/srdf/): is the library used to handle RDF. It is a simple interface with 2 implementations,
  one in [Apache Jena](https://jena.apache.org/), and the other in [RDF4j](https://rdf4j.org/).
- [Any23](https://any23.apache.org/): is used by RDFShape to convert HTML files in RDFa and Microdata to RDF.
- [Topbraid SHACL API](https://github.com/TopQuadrant/shacl): is used to add another SHACL engine apart of the SHaclEX
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
