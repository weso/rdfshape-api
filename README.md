# Tutorial

* [How-to](https://github.com/labra/rdfshape/wiki/Tutorial): explains how to use RDFShape to validate RDF

# Deployed versions of RDFShape

RDFShape is already deployed in two servers:

* [Our local server](http://rdfshape.weso.es): In this server it works ok with large shapes, but the server can be down sometimes given that it is located in a University laboratory and there is no good maintenance.

* [heroku](http://rdfshape.herokuapp.com): In this server, it works almost always although it can take some initial time to wakeup and there are some memory limitations in Heroku free accounts that does it crash for large shapes.


# Installation and Local Deployment 

RDFShape is implemented as a [Play!](https://playframework.com/) application. 
The only requirements are:

* JDK 1.7 or above (It does not work on JDK 1.6)

* [Play! framework](https://playframework.com/)


In order to deploy RDFShape locally, the steps are:

* Install Play activator which is available [here](http://playframework.com/download)

* Clone the github repository which is available [here](https://github.com/labra/rdfshape)

* Go to directory where RDFShape source code is located and execute `activator run`

* After some time downloading and compiling the source code it will start the application, which can be accessed at:  http://localhost:9000

