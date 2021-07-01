(self.webpackChunkrdfshape_api_webdocs=self.webpackChunkrdfshape_api_webdocs||[]).push([[664],{3905:function(e,t,n){"use strict";n.d(t,{Zo:function(){return p},kt:function(){return d}});var r=n(7294);function i(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function a(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function o(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?a(Object(n),!0).forEach((function(t){i(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):a(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,r,i=function(e,t){if(null==e)return{};var n,r,i={},a=Object.keys(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||(i[n]=e[n]);return i}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(i[n]=e[n])}return i}var l=r.createContext({}),c=function(e){var t=r.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):o(o({},t),e)),n},p=function(e){var t=c(e.components);return r.createElement(l.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},g=r.forwardRef((function(e,t){var n=e.components,i=e.mdxType,a=e.originalType,l=e.parentName,p=s(e,["components","mdxType","originalType","parentName"]),g=c(n),d=i,f=g["".concat(l,".").concat(d)]||g[d]||u[d]||a;return n?r.createElement(f,o(o({ref:t},p),{},{components:n})):r.createElement(f,o({ref:t},p))}));function d(e,t){var n=arguments,i=t&&t.mdxType;if("string"==typeof e||i){var a=n.length,o=new Array(a);o[0]=g;var s={};for(var l in t)hasOwnProperty.call(t,l)&&(s[l]=t[l]);s.originalType=e,s.mdxType="string"==typeof e?e:i,o[1]=s;for(var c=2;c<a;c++)o[c]=n[c];return r.createElement.apply(null,o)}return r.createElement.apply(null,n)}g.displayName="MDXCreateElement"},5488:function(e,t,n){"use strict";n.r(t),n.d(t,{frontMatter:function(){return s},metadata:function(){return l},toc:function(){return c},default:function(){return u}});var r=n(2122),i=n(9756),a=(n(7294),n(3905)),o=["components"],s={id:"testing-auditing_logs",title:"Logging system"},l={unversionedId:"api-testing-auditing/testing-auditing_logs",id:"api-testing-auditing/testing-auditing_logs",isDocsHomePage:!1,title:"Logging system",description:"Infrastructure",source:"@site/../rdfshape-docs/target/mdoc/api-testing-auditing/testing-auditing_logs.md",sourceDirName:"api-testing-auditing",slug:"/api-testing-auditing/testing-auditing_logs",permalink:"/rdfshape-api/docs/api-testing-auditing/testing-auditing_logs",version:"current",frontMatter:{id:"testing-auditing_logs",title:"Logging system"},sidebar:"docsSidebar",previous:{title:"Integration tests and other",permalink:"/rdfshape-api/docs/api-testing-auditing/testing-auditing_integration"},next:{title:"About this webpage",permalink:"/rdfshape-api/docs/webpage/webpage_info"}},c=[{value:"Infrastructure",id:"infrastructure",children:[]},{value:"Functionality",id:"functionality",children:[{value:"Console log messages",id:"console-log-messages",children:[]},{value:"File log messages",id:"file-log-messages",children:[]},{value:"Adding custom functionality",id:"adding-custom-functionality",children:[]}]}],p={toc:c};function u(e){var t=e.components,n=(0,i.Z)(e,o);return(0,a.kt)("wrapper",(0,r.Z)({},p,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h2",{id:"infrastructure"},"Infrastructure"),(0,a.kt)("p",null,"This project uses a logging framework provided by two mature libraries:"),(0,a.kt)("ol",null,(0,a.kt)("li",{parentName:"ol"},(0,a.kt)("p",{parentName:"li"},(0,a.kt)("a",{parentName:"p",href:"http://logback.qos.ch/"},"Logback"),": Framework's back-end, provides customizable logging levels and log appenders for\nconsole, files, etc.")),(0,a.kt)("li",{parentName:"ol"},(0,a.kt)("p",{parentName:"li"},(0,a.kt)("a",{parentName:"p",href:"https://github.com/lightbend/scala-logging"},"scala-logging"),": Framework's front-end, reduces the verbosity of logging\nmessages from the code thanks to several macros and utilities."))),(0,a.kt)("h2",{id:"functionality"},"Functionality"),(0,a.kt)("h3",{id:"console-log-messages"},"Console log messages"),(0,a.kt)("p",null,"RDFShape API is configured to use a ",(0,a.kt)("a",{parentName:"p",href:"http://logback.qos.ch/manual/appenders.html#ConsoleAppender"},"Console Appender")," to log\nmessages to the console, refer to ",(0,a.kt)("a",{parentName:"p",href:"/rdfshape-api/docs/api-usage/usage_cli"},"CLI section")," to configure what is logged to\nthe console via CLI arguments."),(0,a.kt)("h3",{id:"file-log-messages"},"File log messages"),(0,a.kt)("p",null,"RDFShape API is configured to use\na ",(0,a.kt)("a",{parentName:"p",href:"http://logback.qos.ch/manual/appenders.html#RollingFileAppender"},"Rolling File Appender")," to store all log messages of\nlevel ",(0,a.kt)("strong",{parentName:"p"},"DEBUG")," and above inside ",(0,a.kt)("em",{parentName:"p"},".log")," files, whether this messages are verbosely shown on console or not."),(0,a.kt)("p",null,"The logs written to the files:"),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("p",{parentName:"li"},"Are located inside a ",(0,a.kt)("inlineCode",{parentName:"p"},"logs")," folder, in the application's execution path. Therefore, make sure you run the app with a\nuser with write access and from a location that is writable.")),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("p",{parentName:"li"},"Follow a ",(0,a.kt)("a",{parentName:"p",href:"http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy"},"time-based rolling policy"),", which\nimplies that logs are rotated and compressed in a periodic basis defined in logback's configuration file."))),(0,a.kt)("h3",{id:"adding-custom-functionality"},"Adding custom functionality"),(0,a.kt)("p",null,"The project is already configured to work as explained above. For further configuration, check\nthe ",(0,a.kt)("a",{parentName:"p",href:"https://github.com/weso/rdfshape-api/blob/master/src/main/resources/logback-configurations/logback.groovy"},"logback.groovy"),"\nconfiguration file or the documentation of each respective library."))}u.isMDXComponent=!0}}]);