# RDFShape

RDF playground. This repository contains the server part of the RDFShape web app.
The server has been implemented in Scala using the [http4s](https://http4s.org/) library. 

[![Build Status](https://travis-ci.org/weso/rdfshape.svg?branch=master)](https://travis-ci.org/weso/rdfshape)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2ad10ec42b6a4bb389aeb114fe192f21)](https://www.codacy.com/gh/weso/rdfshape?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weso/rdfshape&amp;utm_campaign=Badge_Grade)

# More info

* The client part of RDFShape has been separated to a [React app](https://github.com/weso/rdfshape-client).
* Background info about validating RDF: [Validating RDF data book](http://book.validatingrdf.com)
* [How-to](https://github.com/labra/rdfshape/wiki/Tutorial): explains how to use RDFShape to validate RDF

# Deployed versions of RDFShape

RDFShape is already deployed [here](http://rdfshape.weso.es).

# Installation and Local Deployment 

## Requirements

* RDFShape server requires [SBT](https://www.scala-sbt.org/) to be built

## Deploy at local machine

* Clone the [github repository](https://github.com/labra/rdfshape)

* Go to directory where RDFShape source code is located and execute `sbt run`

* After some time downloading and compiling the source code it will start the application, which can be accessed at: [http://localhost:8080](localhost)

* If you want a different port run `sbt "run --server --port <PortNumber>"`

## Build a docker image

* Install [SBT](https://www.scala-sbt.org/)

* Run `sbt docker:publishLocal` which will create a docker file at `target/docker`

# Dependencies

RDFShape server has been implemented in Scala using the following libraries:

* [SHaclEX](https://github.com/labra/shaclex), a Scala implementation of ShEx and SHACL.
* [http4s](https://http4s.org/) a purely functional library for http.
* [cats](https://typelevel.org/cats/) a library for functional programming in Scala.
* [UMLShaclex](https://github.com/labra/shaclex), contains the visualization code that converts schemas to UML diagrams
* [SRDF](http://www.weso.es/srdf/) is the library used to handle RDF. It is a simple interface with 2 implementations, one in [Apache Jena](https://jena.apache.org/), and the other in [RDF4j](https://rdf4j.org/).
* [Any23](https://any23.apache.org/) is used by RDFShape to convert HTML files in RDFa and Microdata to RDF.
* [Topbraid SHACL API](https://github.com/TopQuadrant/shacl) is used to add another SHACL engine apart of the SHaclEX and Apache Jena SHACL engines.

# Contribution and issues

Contributions are greatly appreciated. Please fork this repository and open a pull request to add more features or submit issues:

* [Issues about RDFShape online demo](https://github.com/labra/rdfshape/issues)
* [Issues about SHACLex validation library](https://github.com/labra/shaclex/issues)

<a href="https://github.com/weso/rdfshape/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=weso/rdfshape" />
</a>
