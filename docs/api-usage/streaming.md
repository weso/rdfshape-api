---
id: streaming
title: Streaming validations (beta)
---

# Streaming validations (beta)

@APP_NAME@ features some tools for validating [Kafka](https://kafka.apache.org/)
streams of RDF data instead of static datasets.

You may find more information and try it out using the
following [API endpoint](https://app.swaggerhub.com/apis-docs/weso/RDFShape/#/schema/getSchemaValidateStream)
.

> **Notice this is just a beta version exposed in @APP_NAME@/Clients as a demo**

## Motivation and tools

The processing of streaming validations is part of one of WESO's student's
Master's theis, which involved the development
of [@STREAMING_APP_NAME@](https://ulitol97.github.io/comet/), a library capable
of validating streams of RDF data
using [SHaclEX](https://github.com/weso/shaclex)
under the hood.

For purposes beyond this demo's limitations, we recommend trying
@STREAMING_APP_NAME@ out yourself.

## Usage

As it is streams we are dealing with, the communication with @APP_NAME@'s is
not done through HTTP requests anymore, but
through [WebSockets](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
.

The workflow (simplified) goes as follows:

1. The client attempts to open a WebSockets connection with the server.
2. If the connection attempt succeeds, the server will remain waiting for the
   client.
3. The client may then send a WebSockets message in JSON (see [_Data
   Model_](#dataModel)) requesting the server to perform a certain validation on
   an input RDF data stream.
4. If the client's request is correct, the server will begin the validation,
   sending each output back to the client in separate WebSockets messages.

## Data model

### Request model

For the server to start sending results to a client, it is the latter which has
to first send a request to the server.

These requests are JSON-formatted WebSockets messages, telling the server how
the validation should be performed, including:

- _configuration_: Parent object of the JSON tree.
    - _validator_: Information on how the @STREAMING_APP_NAME@'s should operate.
        - _schema_: Schema that the RDF data will be validated against.
          Formatted as in the rest of API requests.
        - _triggerMode_: Validation trigger that the RDF data will be validated
          against. Formatted as in the rest of API requests.
        - _haltOnInvalid (Optional)_: Whether if the streaming validation should
          stop the moment an incoming item does not validate. Default: `false`.
        - _haltOnErrored (Optional)_: Whether if the streaming validation should
          stop the moment an error occurs during a validation, or just ignore
          it. Default: `false`.
    - _extractor_: Information on how @STREAMING_APP_NAME@'s Kafka extractor
      should operate.
        - _data_: Object with the properties that incoming RDF data is expected
          to have.
            - _format_: Format of the incoming RDF data (Turtle, JSONLD, etc.).
            - _inference (Optional)_: Inference to be applied on the incoming
              RDF data (turtle, JSONLD, etc.). Default: `NONE`.
    - _stream_: Information for @STREAMING_APP_NAME@ to consume an incoming
      Kafka stream.
        - _server_: Hostname/IP address of the Kafka server streaming RDF data.
        - _port_ (Optional): Port from which the Kafka server is streaming data.
          Default: `9092`.
        - _topic_: Topic on which the Kafka server is streaming data.
        - _groupId_ (Optional): Group that the Kafka consumer shall identify
          with. Useful to resume validations where they left off. Default:
          string with the name of the app: (`appName-appVersion`).

```json title="Example client message requesting a streaming validation"
{
  "configuration": {
    "validator": {
      "haltOnInvalid": false,
      "haltOnErrored": false,
      "schema": {
        "content": "PREFIX ex: <http://example.org/>\nPREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n\n# Filters of a valid sensor reading\nex:ValidReading {\n  ex:readingDatetime     xsd:dateTime  ; # Has a VALID timestamp\n  ex:readingTemperature xsd:decimal MININCLUSIVE 18 MAXINCLUSIVE 20 + ; # 1+ readings in range 18-20\n  ex:status [ \"OK\" \"RUNNING\" ] # Status must be one of \n}",
        "engine": "ShEx",
        "format": "ShExC",
        "source": "byText"
      },
      "triggerMode": {
        "shape-map": {
          "content": "ex:reading@ex:ValidReading",
          "format": "Compact",
          "source": "byText"
        },
        "type": "ShapeMap"
      }
    },
    "extractor": {
      "data": {
        "format": "Turtle",
        "inference": "None"
      },
    },
    "stream": {
      "server": "localhost",
      "port": 9092,
      "topic": "rdf",
      "groupId": "myGroup"
    }
  }
}
```

### Response model

Though subject to change, results emitted from @APP_NAME@ to the client
have the following structure:

- _type_: Metadata telling the client the type of content this message has. The
  possible values are:
    - <u>result</u>: the message contains a JSON-formatted validation result.
    - <u>error</u>: the message contains a JSON-formatted error
- _content_: Contents of the message itself
    - For results, these contents will be the validation results.
    - For errors, these contents will be an error description.

```json title="Example server response for a validation result"
{
  "type": "result",
  "content": {
    "valid": true,
    "status": "valid",
    "message": "Data validation was successful",
    "instant": "2022-05-05T15:00:57.925050695Z",
    "report": {
      "valid": true,
      "type": "Result",
      "message": "Validated",
      "shapeMap": [ ... ],
      "errors": [],
      "nodesPrefixMap": { ... },
      "shapesPrefixMap": { ... }
    }
  }
}
```

#### WebSockets stream closure

If the WebSockets client does not disconnect, the streaming validation will keep
running unless:

- An invalid/erroring validation takes places and the validator was configured
  to stop on these cases.
- The validator does not receive any data to validate for a certain time period:
  the WebSockets connection is closed to save resources.

In the event of closure, two WebSocket frames are sent to the client:

1. A standard WebSocket frame containing JSON-formatted text explaining the
   error that prompted the connection to close, including:
    * _type_: will be `error`.
    * _content_:
        * _message_: Simplified error message.
        * _reason_: Detailed cause of the error, only available when the error
          cause is not-validating RDF data, in which case the validation report
          will be included here.
2. A closing WebSocket frame, with a short description of the closure reason and
   its corresponding close code:
   - **3000**: The client's request did not contain valid JSON data.
   - **3001**: The client's request did not contain a valid configuration.
   - **3002**: A validation item was invalid.
   - **3003**: An error occurred while validating an item.
   - **3004**: No items were received for a while.
   - **3005**: The configuration contained invalid values.
   - **3006**: An invalid value was provided to the server.
   - **3007**: An error occurred connecting to the Kafka stream.
   - **4999**: Connection closed due to an unknown error.

```json title="Example last server response before closure"
{
  "type": "error",
  "content": {
    "message": "StreamInvalidItemException - Stream halted because an item was invalid",
    "reason": {
      "valid": false,
      "type": "Result",
      "message": "Validated with errors",
      "shapeMap": [ ... ],
      "errors": [ ... ],
      "nodesPrefixMap": { ... },
      "shapesPrefixMap": { ... }
    }
  }
}
```

