"use strict";(self.webpackChunkrdfshape_api_webdocs=self.webpackChunkrdfshape_api_webdocs||[]).push([[568],{3905:function(e,t,a){a.d(t,{Zo:function(){return p},kt:function(){return c}});var n=a(7294);function r(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function i(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function o(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?i(Object(a),!0).forEach((function(t){r(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):i(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function l(e,t){if(null==e)return{};var a,n,r=function(e,t){if(null==e)return{};var a,n,r={},i=Object.keys(e);for(n=0;n<i.length;n++)a=i[n],t.indexOf(a)>=0||(r[a]=e[a]);return r}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(n=0;n<i.length;n++)a=i[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var s=n.createContext({}),m=function(e){var t=n.useContext(s),a=t;return e&&(a="function"==typeof e?e(t):o(o({},t),e)),a},p=function(e){var t=m(e.components);return n.createElement(s.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},d=n.forwardRef((function(e,t){var a=e.components,r=e.mdxType,i=e.originalType,s=e.parentName,p=l(e,["components","mdxType","originalType","parentName"]),d=m(a),c=r,h=d["".concat(s,".").concat(c)]||d[c]||u[c]||i;return a?n.createElement(h,o(o({ref:t},p),{},{components:a})):n.createElement(h,o({ref:t},p))}));function c(e,t){var a=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var i=a.length,o=new Array(i);o[0]=d;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l.mdxType="string"==typeof e?e:r,o[1]=l;for(var m=2;m<i;m++)o[m]=a[m];return n.createElement.apply(null,o)}return n.createElement.apply(null,a)}d.displayName="MDXCreateElement"},4907:function(e,t,a){a.r(t),a.d(t,{assets:function(){return p},contentTitle:function(){return s},default:function(){return c},frontMatter:function(){return l},metadata:function(){return m},toc:function(){return u}});var n=a(3117),r=a(102),i=(a(7294),a(3905)),o=["components"],l={id:"streaming",title:"Streaming validations (beta)"},s="Streaming validations (beta)",m={unversionedId:"api-usage/streaming",id:"api-usage/streaming",title:"Streaming validations (beta)",description:"RDFShape API features some tools for validating Kafka",source:"@site/../rdfshape-docs/target/mdoc/api-usage/streaming.md",sourceDirName:"api-usage",slug:"/api-usage/streaming",permalink:"/rdfshape-api/docs/api-usage/streaming",tags:[],version:"current",frontMatter:{id:"streaming",title:"Streaming validations (beta)"},sidebar:"docsSidebar",previous:{title:"Command Line Interface",permalink:"/rdfshape-api/docs/api-usage/usage_cli"},next:{title:"Unit tests with Munit",permalink:"/rdfshape-api/docs/api-testing-auditing/testing-auditing_munit"}},p={},u=[{value:"Motivation and tools",id:"motivation-and-tools",level:2},{value:"Usage",id:"usage",level:2},{value:"Data model",id:"data-model",level:2},{value:"Request model",id:"request-model",level:3},{value:"Response model",id:"response-model",level:3},{value:"WebSockets stream closure",id:"websockets-stream-closure",level:4}],d={toc:u};function c(e){var t=e.components,a=(0,r.Z)(e,o);return(0,i.kt)("wrapper",(0,n.Z)({},d,a,{components:t,mdxType:"MDXLayout"}),(0,i.kt)("h1",{id:"streaming-validations-beta"},"Streaming validations (beta)"),(0,i.kt)("p",null,"RDFShape API features some tools for validating ",(0,i.kt)("a",{parentName:"p",href:"https://kafka.apache.org/"},"Kafka"),"\nstreams of RDF data instead of static datasets."),(0,i.kt)("p",null,"You may find more information and try it out using the\nfollowing ",(0,i.kt)("a",{parentName:"p",href:"https://app.swaggerhub.com/apis-docs/weso/RDFShape/#/schema/getSchemaValidateStream"},"API endpoint"),"\n."),(0,i.kt)("blockquote",null,(0,i.kt)("p",{parentName:"blockquote"},(0,i.kt)("strong",{parentName:"p"},"Notice this is just a beta version exposed in RDFShape API/Clients as a demo"))),(0,i.kt)("h2",{id:"motivation-and-tools"},"Motivation and tools"),(0,i.kt)("p",null,"The processing of streaming validations is part of one of WESO's student's\nMaster's theis, which involved the development\nof ",(0,i.kt)("a",{parentName:"p",href:"https://ulitol97.github.io/comet/"},"Comet"),", a library capable\nof validating streams of RDF data\nusing ",(0,i.kt)("a",{parentName:"p",href:"https://github.com/weso/shaclex"},"SHaclEX"),"\nunder the hood."),(0,i.kt)("p",null,"For purposes beyond this demo's limitations, we recommend trying\nComet out yourself."),(0,i.kt)("h2",{id:"usage"},"Usage"),(0,i.kt)("p",null,"As it is streams we are dealing with, the communication with RDFShape API's is\nnot done through HTTP requests anymore, but\nthrough ",(0,i.kt)("a",{parentName:"p",href:"https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API"},"WebSockets"),"\n."),(0,i.kt)("p",null,"The workflow (simplified) goes as follows:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},"The client attempts to open a WebSockets connection with the server."),(0,i.kt)("li",{parentName:"ol"},"If the connection attempt succeeds, the server will remain waiting for the\nclient."),(0,i.kt)("li",{parentName:"ol"},"The client may then send a WebSockets message in JSON (see ",(0,i.kt)("a",{parentName:"li",href:"#dataModel"},(0,i.kt)("em",{parentName:"a"},"Data\nModel")),") requesting the server to perform a certain validation on\nan input RDF data stream."),(0,i.kt)("li",{parentName:"ol"},"If the client's request is correct, the server will begin the validation,\nsending each output back to the client in separate WebSockets messages.")),(0,i.kt)("h2",{id:"data-model"},"Data model"),(0,i.kt)("h3",{id:"request-model"},"Request model"),(0,i.kt)("p",null,"For the server to start sending results to a client, it is the latter which has\nto first send a request to the server."),(0,i.kt)("p",null,"These requests are JSON-formatted WebSockets messages, telling the server how\nthe validation should be performed, including:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"configuration"),": Parent object of the JSON tree.",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"validator"),": Information on how the Comet's should operate.",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"schema"),": Schema that the RDF data will be validated against.\nFormatted as in the rest of API requests."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"triggerMode"),": Validation trigger that the RDF data will be validated\nagainst. Formatted as in the rest of API requests."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"haltOnInvalid (Optional)"),": Whether if the streaming validation should\nstop the moment an incoming item does not validate. Default: ",(0,i.kt)("inlineCode",{parentName:"li"},"false"),"."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"haltOnErrored (Optional)"),": Whether if the streaming validation should\nstop the moment an error occurs during a validation, or just ignore\nit. Default: ",(0,i.kt)("inlineCode",{parentName:"li"},"false"),"."))),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"extractor"),": Information on how Comet's Kafka extractor\nshould operate.",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"data"),": Object with the properties that incoming RDF data is expected\nto have.",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"format"),": Format of the incoming RDF data (Turtle, JSONLD, etc.)."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"inference (Optional)"),": Inference to be applied on the incoming\nRDF data (turtle, JSONLD, etc.). Default: ",(0,i.kt)("inlineCode",{parentName:"li"},"NONE"),"."))))),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"stream"),": Information for Comet to consume an incoming\nKafka stream.",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"server"),": Hostname/IP address of the Kafka server streaming RDF data."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"port")," (Optional): Port from which the Kafka server is streaming data.\nDefault: ",(0,i.kt)("inlineCode",{parentName:"li"},"9092"),"."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"topic"),": Topic on which the Kafka server is streaming data.")))))),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json",metastring:'title="Example client message requesting a streaming validation"',title:'"Example',client:!0,message:!0,requesting:!0,a:!0,streaming:!0,'validation"':!0},'{\n  "configuration": {\n    "validator": {\n      "haltOnInvalid": false,\n      "haltOnErrored": false,\n      "schema": {\n        "content": "PREFIX ex: <http://example.org/>\\nPREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\\n\\n# Filters of a valid sensor reading\\nex:ValidReading {\\n  ex:readingDatetime     xsd:dateTime  ; # Has a VALID timestamp\\n  ex:readingTemperature xsd:decimal MININCLUSIVE 18 MAXINCLUSIVE 20 + ; # 1+ readings in range 18-20\\n  ex:status [ \\"OK\\" \\"RUNNING\\" ] # Status must be one of \\n}",\n        "engine": "ShEx",\n        "format": "ShExC",\n        "source": "byText"\n      },\n      "triggerMode": {\n        "shapeMap": {\n          "content": "ex:reading@ex:ValidReading",\n          "format": "Compact",\n          "source": "byText"\n        },\n        "type": "ShapeMap"\n      }\n    },\n    "extractor": {\n      "data": {\n        "format": "Turtle",\n        "inference": "None"\n      },\n    },\n    "stream": {\n      "server": "localhost",\n      "port": 9092,\n      "topic": "rdf"\n    }\n  }\n}\n')),(0,i.kt)("h3",{id:"response-model"},"Response model"),(0,i.kt)("p",null,"Though subject to change, results emitted from RDFShape API to the client\nhave the following structure:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"type"),": Metadata telling the client the type of content this message has. The\npossible values are:",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("u",null,"result"),": the message contains a JSON-formatted validation result."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("u",null,"error"),": the message contains a JSON-formatted error"))),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"content"),": Contents of the message itself",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},"For results, these contents will be the validation results."),(0,i.kt)("li",{parentName:"ul"},"For errors, these contents will be an error description.")))),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json",metastring:'title="Example server response for a validation result"',title:'"Example',server:!0,response:!0,for:!0,a:!0,validation:!0,'result"':!0},'{\n  "type": "result",\n  "content": {\n    "valid": true,\n    "status": "valid",\n    "message": "Data validation was successful",\n    "instant": "2022-05-05T15:00:57.925050695Z",\n    "report": {\n      "valid": true,\n      "type": "Result",\n      "message": "Validated",\n      "shapeMap": [ ... ],\n      "errors": [],\n      "nodesPrefixMap": { ... },\n      "shapesPrefixMap": { ... }\n    }\n  }\n}\n')),(0,i.kt)("h4",{id:"websockets-stream-closure"},"WebSockets stream closure"),(0,i.kt)("p",null,"If the WebSockets client does not disconnect, the streaming validation will keep\nrunning unless:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"An invalid/erroring validation takes places and the validator was configured\nto stop on these cases."),(0,i.kt)("li",{parentName:"ul"},"The validator does not receive any data to validate for a certain time period:\nthe WebSockets connection is closed to save resources.")),(0,i.kt)("p",null,"In the event of closure, two WebSocket frames are sent to the client:"),(0,i.kt)("ol",null,(0,i.kt)("li",{parentName:"ol"},"A standard WebSocket frame containing JSON-formatted text explaining the\nerror that prompted the connection to close, including:",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"type"),": will be ",(0,i.kt)("inlineCode",{parentName:"li"},"error"),"."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"content"),":",(0,i.kt)("ul",{parentName:"li"},(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"message"),": Simplified error message."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("em",{parentName:"li"},"reason"),": Detailed cause of the error, only available when the error\ncause is not-validating RDF data, in which case the validation report\nwill be included here."))))),(0,i.kt)("li",{parentName:"ol"},"A closing WebSocket frame, with a short description of the closure reason and\nits corresponding close code.")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-json",metastring:'title="Example last server response before closure"',title:'"Example',last:!0,server:!0,response:!0,before:!0,'closure"':!0},'{\n  "type": "error",\n  "content": {\n    "message": "StreamInvalidItemException - Stream halted because an item was invalid",\n    "reason": {\n      "valid": false,\n      "type": "Result",\n      "message": "Validated with errors",\n      "shapeMap": [ ... ],\n      "errors": [ ... ],\n      "nodesPrefixMap": { ... },\n      "shapesPrefixMap": { ... }\n    }\n  }\n}\n')))}c.isMDXComponent=!0}}]);