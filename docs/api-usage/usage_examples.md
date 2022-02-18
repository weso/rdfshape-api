---
id: usage_examples
title: Example API Requests
---

# Example API requests

## Data info

```
curl -i -X POST -H "Content-type:multipart/form-data" \
'https://api.rdfshape.weso.es/api/data/info' \
--form-string 'data=@prefix :      <http://example.org/> . @prefix foaf:  <http://xmlns.com/foaf/0.1/> . :alice  a       foaf:Person .' \
--form-string 'dataSource=byText' \
--form-string 'dataFormat=turtle' \
--form-string 'dataInference=None'
```

<hr/>

#### Pending examples

## Validation

## Wikidata

## Convert RDF data to JSON representation

The JSON representation follows the format used by
the [Cytoscape component](https://github.com/plotly/react-cytoscapejs).
