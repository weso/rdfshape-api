---
id: usage_tutorial
title: API Tutorial
---

# API Tutorial

@APP_NAME@ is a public API that can be used as a playground for RDF, Shape Expressions, SHACL, etc.

## ShEx

Validate RDF data with ShEx. Example from the [Validating RDF book](https://book.validatingrdf.com/):

- [Example](https://rdfshape.weso.es/shExValidate?activeSchemaTab=%23schemaTextArea&activeTab=%23dataTextArea&data=PREFIX%20%3A%20%20%20%20%20%20%20%3Chttp%3A%2F%2Fexample.org%2F%3E%0APREFIX%20schema%3A%20%3Chttp%3A%2F%2Fschema.org%2F%3E%0APREFIX%20xsd%3A%20%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23%3E%0APREFIX%20foaf%3A%20%20%20%3Chttp%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2F%3E%0A%0A%3Aalice%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Alice%22%20%3B%20%20%20%20%20%20%20%20%20%20%20%20%23%20%25%2A%20%5CPasses%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AFemale%20%3B%0A%20%20%20%20%20%20%20schema%3Aknows%20%20%20%20%20%20%20%20%20%20%3Abob%20.%0A%0A%3Abob%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AMale%20%3B%20%20%20%20%20%20%20%20%23%20%25%2A%20%5CPasses%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Robert%22%3B%20%20%20%20%20%20%20%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3AbirthDate%20%20%20%20%20%20%221980-03-10%22%5E%5Exsd%3Adate%20.%0A%0A%3Acarol%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Carol%22%20%3B%20%20%20%20%20%20%20%20%20%20%20%20%23%20%25%2A%20%5CPasses%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20%22unspecified%22%20%3B%20%20%0A%20%20%20%20%20%20%20foaf%3Aname%20%20%20%20%20%20%20%20%20%20%20%20%20%22Carol%22%20.%0A%0A%3Adave%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Dave%22%3B%20%20%20%20%20%20%20%20%20%23%20%25%2A%20%5CFails%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20%22XYY%22%3B%20%20%20%20%20%20%20%20%20%20%23%0A%20%20%20%20%20%20%20schema%3AbirthDate%20%20%20%20%20%201980%20.%20%20%20%20%20%20%20%20%20%20%23%20%25%2A%201980%20is%20not%20an%20xsd%3Adate%20%2A%29%0A%0A%3Aemily%20schema%3Aname%20%22Emily%22%2C%20%22Emilee%22%20%3B%20%20%20%20%20%20%20%23%20%25%2A%20%5CFails%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AFemale%20.%20%23%20%25%2A%20too%20many%20schema%3Anames%20%2A%29%0A%0A%3Afrank%20foaf%3Aname%20%20%20%20%20%20%20%20%20%20%20%20%20%22Frank%22%20%3B%20%20%20%20%20%20%20%23%20%25%2A%20%5CFails%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%3A%20%20%20%20%20%20%20%20schema%3AMale%20.%20%20%20%23%20%25%2A%20missing%20schema%3Aname%20%2A%29%0A%0A%3Agrace%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Grace%22%20%3B%20%20%20%20%20%20%20%23%20%25%2A%20%5CFails%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AMale%20%3B%20%20%20%23%20%0A%20%20%20%20%20%20%20schema%3Aknows%20%20%20%20%20%20%20%20%20%20_%3Ax%20.%20%20%20%20%20%20%20%20%20%20%20%23%20%25%2A%20%5C_%3Ax%20is%20not%20an%20IRI%20%2A%29%0A%0A%3Aharold%20schema%3Aname%20%20%20%20%20%20%20%20%20%22Harold%22%20%3B%20%20%20%20%23%20%25%2A%20%5CFails%7B%3AUser%7D%20%2A%29%0A%20%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20schema%3AMale%20%3B%20%0A%20%20%20%20%20%20%20%20schema%3Aknows%20%20%20%20%20%20%20%20%3Agrace%20.%20%20%20%20%20%20%23%20%25%2A%20%3Agrace%20does%20not%20conform%20to%20%3AUser%20%2A%29&dataFormat=TURTLE&dataFormatTextArea=TURTLE&endpoint=&inference=None&schema=PREFIX%20%3A%20%20%20%20%20%20%20%3Chttp%3A%2F%2Fexample.org%2F%3E%0APREFIX%20schema%3A%20%3Chttp%3A%2F%2Fschema.org%2F%3E%0APREFIX%20xsd%3A%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23%3E%0A%0A%3AUser%20%7B%0A%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20xsd%3Astring%20%20%3B%0A%20%20schema%3AbirthDate%20%20%20%20%20xsd%3Adate%3F%20%20%3B%0A%20%20schema%3Agender%20%20%20%20%20%20%20%20%5B%20schema%3AMale%20schema%3AFemale%20%5D%20OR%20xsd%3Astring%20%3B%0A%20%20schema%3Aknows%20%20%20%20%20%20%20%20%20IRI%20%40%3AUser%2A%0A%7D&schemaEngine=ShEx&schemaFormat=ShExC&shapeMap=%3Aalice%40%3AUser%2C%3Abob%40%3AUser%2C%3Acarol%40%3AUser%2C%3Aemily%40%3AUser%2C%3Afrank%40%3AUser%2C%3Agrace%40%3AUser%2C%3Aharold%40%3AUser&shapeMapActiveTab=%23shapeMapTextArea&shapeMapFormat=Compact&triggerMode=shapeMap)

## SHACL

Validate RDF data with SHACL. Example from the Validating RDF book:

- [Example](https://rdfshape.weso.es/shaclValidate?activeSchemaTab=%23schemaTextArea&activeTab=%23dataTextArea&data=%40prefix%20%3A%20%20%20%20%20%20%20%3Chttp%3A%2F%2Fexample.org%2F%3E%20.%0A%40prefix%20sh%3A%20%20%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2Fns%2Fshacl%23%3E%20.%0A%40prefix%20xsd%3A%20%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23%3E%20.%0A%40prefix%20schema%3A%20%3Chttp%3A%2F%2Fschema.org%2F%3E%20.%0A%40prefix%20foaf%3A%20%20%20%3Chttp%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2F%3E%20.%0A%40prefix%20rdfs%3A%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%20.%0A%20%20%20%20%20%20%20%20%0A%0A%3Aalice%20a%20%3AUser%3B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%25%2A%5CPasses%7B%3AUserShape%7D%20%2A%29%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Alice%22%20%3B%20%20%20%20%20%20%20%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AFemale%20%3B%0A%20%20%20%20%20%20%20schema%3Aknows%20%20%20%20%20%20%20%20%20%20%3Abob%20.%0A%0A%3Abob%20%20%20a%20%3AUser%3B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%25%2A%5CPasses%7B%3AUserShape%7D%20%2A%29%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AMale%20%3B%20%20%20%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Robert%22%3B%0A%20%20%20%20%20%20%20schema%3AbirthDate%20%20%20%20%20%20%221980-03-10%22%5E%5Exsd%3Adate%20.%0A%0A%3Acarol%20a%20%3AUser%3B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%25%2A%5CPasses%7B%3AUserShape%7D%20%2A%29%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Aname%20%20%20%20%20%20%20%20%20%20%20%22Carol%22%20%3B%20%20%20%20%20%20%20%20%20%20%20%20%0A%20%20%20%20%20%20%20schema%3Agender%20%20%20%20%20%20%20%20%20schema%3AFemale%20%3B%20%20%0A%20%20%20%20%20%20%20foaf%3Aname%20%20%20%20%20%20%20%20%20%20%20%20%20%22Carol%22%20.&dataFormat=TURTLE&dataFormatTextArea=TURTLE&endpoint=&inference=None&schema=%40prefix%20%3A%20%20%20%20%20%20%20%3Chttp%3A%2F%2Fexample.org%2F%3E%20.%0A%40prefix%20sh%3A%20%20%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2Fns%2Fshacl%23%3E%20.%0A%40prefix%20xsd%3A%20%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23%3E%20.%0A%40prefix%20schema%3A%20%3Chttp%3A%2F%2Fschema.org%2F%3E%20.%0A%40prefix%20foaf%3A%20%20%20%3Chttp%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2F%3E%20.%0A%40prefix%20rdfs%3A%20%20%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%20.%0A%20%20%20%20%20%20%20%20%0A%3AUserShape%20a%20sh%3ANodeShape%3B%0A%20%20%20sh%3AtargetClass%20%3AUser%20%3B%0A%20%20%20sh%3Aproperty%20%5B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20Blank%20node%201%0A%20%20%20%20sh%3Apath%20%20%20%20%20schema%3Aname%20%3B%20%0A%20%20%20%20sh%3AminCount%201%3B%20%0A%20%20%20%20sh%3AmaxCount%201%3B%0A%20%20%20%20sh%3Adatatype%20xsd%3Astring%20%3B%0A%20%20%5D%20%3B%0A%20%20sh%3Aproperty%20%5B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20Blank%20node%202%0A%20%20%20sh%3Apath%20schema%3Agender%20%3B%0A%20%20%20sh%3AminCount%201%3B%0A%20%20%20sh%3AmaxCount%201%3B%0A%20%20%20sh%3Aor%20%28%0A%20%20%20%20%5B%20sh%3Ain%20%28schema%3AMale%20schema%3AFemale%29%20%5D%0A%20%20%20%20%5B%20sh%3Adatatype%20xsd%3Astring%5D%0A%20%20%20%29%0A%20%20%5D%20%3B%0A%20%20sh%3Aproperty%20%5B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20Blank%20node%203%20%20%0A%20%20%20sh%3Apath%20%20%20%20%20schema%3AbirthDate%20%3B%20%0A%20%20%20sh%3AmaxCount%201%3B%20%0A%20%20%20sh%3Adatatype%20xsd%3Adate%20%3B%0A%20%20%5D%20%3B%0A%20%20sh%3Aproperty%20%5B%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20Blank%20node%204%20%0A%20%20%20sh%3Apath%20%20%20%20%20schema%3Aknows%20%3B%20%0A%20%20%20sh%3AnodeKind%20sh%3AIRI%20%3B%0A%20%20%20sh%3Aclass%20%20%20%20%3AUser%20%3B%0A%20%20%5D%20.&schemaEngine=JenaSHACL&schemaFormat=TURTLE&schemaInference=none&triggerMode=targetDecls)

## Data + Schema

- If the _Schema_ option is selected, then @APP_NAME@ validates both the syntax and the shape of RDF graphs.
- The Schema can be entered by URI, by file or by text input.
- Once the validation has finished, the system can generate a shareable permalink with the result.
- For visualizations, the system can also generate a link for embedding the visualizations.

## Data + Schema + Node

- In this option, the system asks for a node in the RDF Graph that will act as the pointed node in the graph to start
  the validation process

## Data Conversions

- This option allows to convert between different RDF formats.
- The available formats are:
    * Turtle
    * RDF/XML
    * RDF/JSON
    * N-Triples

## Schema Conversions

- This option can be used to convert different representations of schema data.
- The available formats are:
    * ShExC (Shape expression compact syntax)
    * The RDF formats: Turtle, RDF/XML, RDF/JSON, N-TRIPLES, etc.

## Further info

@APP_NAME@ is based on [SHaclEX](http://github.com/weso/shaclex/), a Shape Expressions processor which can also be used as
a command line tool. 