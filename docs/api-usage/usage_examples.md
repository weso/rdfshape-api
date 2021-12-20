---
id: usage_examples
title: Example API Requests
---

# Example API requests

## Validate (GET)

```
curl https://api.rdfshape.weso.es/api/schema/validate -G --data-urlencode 'data=<x> <p> 1 .' --data-urlencode 'schema=<S> { <p> . }' --data-urlencode 'shapeMap=<x>@<S>' --data-urlencode 'triggerMode=shapeMap' --data-urlencode 'engine=ShExC'
```

## Validate (POST with file input)

```
curl -X POST -F 'data=@data.ttl' -F 'schema=@data.shex' -F 'shapeMap=@data.shapeMap' -F 'triggerMode=shapeMap' -F 'engine=ShEx' https://api.rdfshape.weso.es/api/schema/validate
```

## Wikidata examples

The following example attempts to validate a wikidata entity with an entity schema. The parameter `-F` (`--form`) was
not used because the `<` symbol has a special symbol for curl, so we had to use `--form-string`.

The following example invokes the validation API to check if item `Q42` (Douglas Adams) validates as `E42` (Author).

```
curl -i -X POST -H "Content-type:multipart/form-data" 'https://api.rdfshape.weso.es/api/wikidata/validate' \
                                      --form-string 'item=<http://www.wikidata.org/entity/Q42>' \
                                      --form-string 'entitySchema=E42'
```

## Convert RDF data to obtain a JSON representation

The JSON representation follows the format used by
the [Cytoscape component](https://github.com/plotly/react-cytoscapejs).

```
curl -k -i -X POST -H "Content-type:multipart/form-data" 'https://api.rdfshape.weso.es/api/data/convert' \
  --form-string 'data=http://tb.plazi.org/GgServer/rdf/9D767B515A0BFFC3C0F7919FF301FC8D' \
  --form-string 'dataFormat=rdf/xml' --form-string 'dataSource=byUrl' --form-string 'targetDataFormat=JSON' \
```

Notice that as we are querying the @API_URL@ service, which requires
an SSL connection, we use option `-k`.