(self.webpackChunkrdfshape_api_webdocs=self.webpackChunkrdfshape_api_webdocs||[]).push([[955],{3905:function(e,t,a){"use strict";a.d(t,{Zo:function(){return u},kt:function(){return m}});var r=a(7294);function n(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function i(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,r)}return a}function o(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?i(Object(a),!0).forEach((function(t){n(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):i(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function p(e,t){if(null==e)return{};var a,r,n=function(e,t){if(null==e)return{};var a,r,n={},i=Object.keys(e);for(r=0;r<i.length;r++)a=i[r],t.indexOf(a)>=0||(n[a]=e[a]);return n}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)a=i[r],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(n[a]=e[a])}return n}var s=r.createContext({}),l=function(e){var t=r.useContext(s),a=t;return e&&(a="function"==typeof e?e(t):o(o({},t),e)),a},u=function(e){var t=l(e.components);return r.createElement(s.Provider,{value:t},e.children)},d={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},c=r.forwardRef((function(e,t){var a=e.components,n=e.mdxType,i=e.originalType,s=e.parentName,u=p(e,["components","mdxType","originalType","parentName"]),c=l(a),m=n,f=c["".concat(s,".").concat(m)]||c[m]||d[m]||i;return a?r.createElement(f,o(o({ref:t},u),{},{components:a})):r.createElement(f,o({ref:t},u))}));function m(e,t){var a=arguments,n=t&&t.mdxType;if("string"==typeof e||n){var i=a.length,o=new Array(i);o[0]=c;var p={};for(var s in t)hasOwnProperty.call(t,s)&&(p[s]=t[s]);p.originalType=e,p.mdxType="string"==typeof e?e:n,o[1]=p;for(var l=2;l<i;l++)o[l]=a[l];return r.createElement.apply(null,o)}return r.createElement.apply(null,a)}c.displayName="MDXCreateElement"},8234:function(e,t,a){"use strict";a.r(t),a.d(t,{frontMatter:function(){return p},metadata:function(){return s},toc:function(){return l},default:function(){return d}});var r=a(2122),n=a(9756),i=(a(7294),a(3905)),o=["components"],p={id:"usage_examples",title:"Example API Requests"},s={unversionedId:"api-usage/usage_examples",id:"api-usage/usage_examples",isDocsHomePage:!1,title:"Example API requests",description:"Validate (GET)",source:"@site/../rdfshape-docs/target/mdoc/api-usage/usage_examples.md",sourceDirName:"api-usage",slug:"/api-usage/usage_examples",permalink:"/rdfshape-api/docs/api-usage/usage_examples",version:"current",frontMatter:{id:"usage_examples",title:"Example API Requests"},sidebar:"docsSidebar",previous:{title:"API Tutorial",permalink:"/rdfshape-api/docs/api-usage/usage_tutorial"},next:{title:"Unit tests with Munit",permalink:"/rdfshape-api/docs/api-testing-auditing/testing-auditing_munit"}},l=[{value:"Validate (GET)",id:"validate-get",children:[]},{value:"Validate (POST with file input)",id:"validate-post-with-file-input",children:[]},{value:"Wikidata examples",id:"wikidata-examples",children:[]},{value:"Convert RDF data to obtain a JSON representation",id:"convert-rdf-data-to-obtain-a-json-representation",children:[]}],u={toc:l};function d(e){var t=e.components,a=(0,n.Z)(e,o);return(0,i.kt)("wrapper",(0,r.Z)({},u,a,{components:t,mdxType:"MDXLayout"}),(0,i.kt)("h2",{id:"validate-get"},"Validate (GET)"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"curl https://api.rdfshape.weso.es/api/schema/validate -G --data-urlencode 'data=<x> <p> 1 .' --data-urlencode 'schema=<S> { <p> . }' --data-urlencode 'shapeMap=<x>@<S>' --data-urlencode 'triggerMode=shapeMap' --data-urlencode 'engine=ShExC'\n")),(0,i.kt)("h2",{id:"validate-post-with-file-input"},"Validate (POST with file input)"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"curl -X POST -F 'data=@data.ttl' -F 'schema=@data.shex' -F 'shapeMap=@data.shapeMap' -F 'triggerMode=ShapeMap' -F 'engine=ShEx' https://api.rdfshape.weso.es/api/schema/validate\n")),(0,i.kt)("h2",{id:"wikidata-examples"},"Wikidata examples"),(0,i.kt)("p",null,"The following example attempts to validate a wikidata entity with an entity schema. The parameter ",(0,i.kt)("inlineCode",{parentName:"p"},"-F")," (",(0,i.kt)("inlineCode",{parentName:"p"},"--form"),") was\nnot used because the ",(0,i.kt)("inlineCode",{parentName:"p"},"<")," symbol has a special symbol for curl, so we had to use ",(0,i.kt)("inlineCode",{parentName:"p"},"--form-string"),"."),(0,i.kt)("p",null,"The following example invokes the validation API to check if item ",(0,i.kt)("inlineCode",{parentName:"p"},"Q42")," (Douglas Adams) validates as ",(0,i.kt)("inlineCode",{parentName:"p"},"E42")," (Author)."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"curl -i -X POST -H \"Content-type:multipart/form-data\" 'https://api.rdfshape.weso.es/api/wikidata/validate' \\\n                                      --form-string 'item=<http://www.wikidata.org/entity/Q42>' \\\n                                      --form-string 'entitySchema=E42'\n")),(0,i.kt)("h2",{id:"convert-rdf-data-to-obtain-a-json-representation"},"Convert RDF data to obtain a JSON representation"),(0,i.kt)("p",null,"The JSON representation follows the format used by\nthe ",(0,i.kt)("a",{parentName:"p",href:"https://github.com/plotly/react-cytoscapejs"},"Cytoscape component"),"."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},"curl -k -i -X POST -H \"Content-type:multipart/form-data\" 'https://api.rdfshape.weso.es/api/data/convert' \\\n  --form-string 'dataURL=http://tb.plazi.org/GgServer/rdf/9D767B515A0BFFC3C0F7919FF301FC8D' \\\n  --form-string 'dataFormatUrl=rdf/xml' --form-string 'targetDataFormat=JSON' \\\n")),(0,i.kt)("p",null,"Notice that as we are querying the ",(0,i.kt)("a",{parentName:"p",href:"https://api.rdfshape.weso.es"},"https://api.rdfshape.weso.es")," service, which requires\na SSL connection, we use option ",(0,i.kt)("inlineCode",{parentName:"p"},"-k"),"."))}d.isMDXComponent=!0}}]);