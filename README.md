# More info

* Background info: [Validating RDF data book](http://book.validatingrdf.com)
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

* If you want a different port run `sbt run --port <PortNumber>`

## Build a docker image

* Install [SBT](https://www.scala-sbt.org/)

* Run `sbt docker:publishLocal` which will create a docker file at `target/docker`

# Dependencies

RDFShape server has been implemented in Scala using the following libraries:

* [SHACLex](https://github.com/labra/shaclex), a Scala implementation of ShEx and SHACL.
* [http4s](https://http4s.org/) a purely functional library for http.
* [cats](https://typelevel.org/cats/) a library for functional programming in Scala.
* [UMLShaclex](https://github.com/labra/shaclex), contains the visualization code that converts schemas to UML diagrams

# Contribution and issues

Contributions are greatly appreciated. Please fork this repository and open a pull request to add more features or submit issues:

* [Issues about RDFShape online demo](https://github.com/labra/rdfshape/issues)
* [Issues about SHACLex validation library](https://github.com/labra/shaclex/issues)
