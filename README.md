# More info

* [Validating RDF data book](http://book.validatingrdf.com)
* [How-to](https://github.com/labra/rdfshape/wiki/Tutorial): explains how to use RDFShape to validate RDF

# Deployed versions of RDFShape

RDFShape is already deployed [here](http://rdfshape.weso.es).

# Installation and Local Deployment 

RDFShape is implemented in Scala using [SBT](https://www.scala-sbt.org/).

In order to deploy RDFShape locally, the steps are:

* Clone the github repository which is available [here](https://github.com/labra/rdfshape)

* Go to directory where RDFShape source code is located and execute `sbt "run --server"`

* After some time downloading and compiling the source code it will start the application, which can be accessed at:  http://localhost:8080

# Dependencies

RDFShape depends on:

* [SHACLex](https://github.com/labra/shaclex), a Scala implementation of ShEx and SHACL.
* [http4s](https://http4s.org/) a purely functional library for http.
* [cats](https://typelevel.org/cats/) a library for functional programming in Scala.

# Contribution and issues

Contributions are greatly appreciated. Please fork this repository and open a pull request to add more features or submit issues:

* [Issues about RDFShape online demo](https://github.com/labra/rdfshape/issues)
* [Issues about SHACLex validation library](https://github.com/labra/shaclex/issues)
