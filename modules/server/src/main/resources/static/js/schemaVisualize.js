var codeMirrorData ;
var codeMirrorSchema ;
var codeMirrorShapeMap ;

function changeMode(element,syntax) {
    var mode = "turtle";
    switch (syntax.toUpperCase()) {
        case "TURTLE": mode = "turtle" ;
            break ;
        case "N-TRIPLES": mode = "turtle" ;
            break ;
        case "RDF/XML": mode = "xml" ;
            break ;
        case "TRIX": mode = "xml" ;
            break ;
        case "SHEXJ" : mode = "javascript" ;
            break ;
        case "RDF/JSON" : mode = "javascript" ;
            break ;
        case "JSON-LD" : mode = "javascript" ;
            break ;
        case "SHEXC": mode = "shex" ;
            break ;
    }
    element.setOption("mode",mode);
}

function changeTheme(theme) {
    codeMirrorSchema.setOption("theme",theme);
}

function hideShowSchema(show) {
   if (show) {
        $("#schemaDiv").hide();
    } else {
        $("#schemaDiv").show();
    }
}

function changeSchemaEmbedded(cb) {
    console.log("changeSchemaEmbedded: " + cb);
    console.log(cb);
    hideShowSchema(cb.checked);
}

$(document).ready(function(){

function getHost() {
    var port = window.location.port;
    return window.location.protocol + "//" +
           window.location.hostname + (port? ":" + port: "") ;
}

function resetResult(result) {
    console.log("Reset result: " + JSON.stringify(result));
    $("#resultDiv").empty();
    $("#resultDiv").data("result", result);
}


function showResult(result) {
    result = $("#resultDiv").data("result");
    console.log("Show result: " + JSON.stringify(result));
    if(result) {
        if (result.schemaName && result.schemaEngine) {
            var schemaName = $("<p/>").append("Schema: " + result.schemaName + ", " + result.schemaEngine);
            $("#resultDiv").append(schemaName);
        }
        if (result.wellFormed) {
            var wellFormed = $("<p/>").append("Wellformed: " + result.wellFormed);
            $("#resultDiv").append(wellFormed);
        } else {
            var wellFormed = $("<p/>").append("Wellformed: " + result.wellFormed);
            $("#resultDiv").append(wellFormed);
            if (result.errors) {
                console.log(result.errors);
                result.errors.forEach(function (e) {
                    var errorMsg = $("<p/>").text(JSON.stringify(e));
                    $("#resultDiv").append(errorMsg);
                })
            }
        }
        if (result.svg) {
            console.log(result.svg);
            var svgDiv = $("<div/>").append(result.svg);
            $("#resultDiv").append(svgDiv);
        }
        var pre = $("<pre/>").text(JSON.stringify(result,undefined,2));
        var details = $("<details/>").append(pre);
        $("#resultDiv").append(details);
    }
}

function getDataFormat(element) {
    var format = element.options[element.selectedIndex].value;
    window.alert("Data format of " + element + " format: " + format);
}

  var urlShaclex = getHost();
  console.log("urlShaclex: " + urlShaclex);

  // When loading document get result from data-result attribute and show it
  var result = $("#resultDiv").data("result");
  showResult(result);

  var schema = document.getElementById("schema")
  if (schema) {
     codeMirrorSchema = CodeMirror.fromTextArea(schema, {
         lineNumbers: true,
         mode: "shex",
         viewportMargin: Infinity,
         matchBrackets: true
     });
    }


 $('.schemaPanel a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
   var name = $(e.target).attr("href");
   console.log("New schema tab: " + name);
   $('#activeSchemaTab').val(name);
 })

 $("#permalink").click(function(e) {
  e.preventDefault();
  console.log("click on permalink...");
     var schema = codeMirrorSchema.getValue();
     var schemaFormat = "";
     var schemaPart = "";
     var activeSchemaTab = $("#activeSchemaTab").attr("value");
     switch (activeSchemaTab) {
         case "#schemaTextArea":
             schemaFormat = $("#schemaFormatTextArea").find(":selected").text();
             schemaPart = "schema=" + encodeURIComponent(schema) ;
             break;
         case "#schemaFile":
             schemaFormat = $("#schemaFormatFile").find(":selected").text();
             schemaPart = "schema=" + encodeURIComponent(schema) ;
             break;
         case "#schemaUrl":
             schemaFormat = $("#schemaFormatUrl").find(":selected").text();
             var schemaURL = $("#schemaURL").val();
             schemaPart = "schemaURL=" + encodeURIComponent(schemaURL) ;
             break;
         default:
             console.log("Unknown value of activeSchemaTab:" + activeSchemaTab);
             schemaFormat = $("#schemaFormatTextArea").find(":selected").text();
             schemaPart = "schema=" + encodeURIComponent(schema) ;
             break;
     }
    var schemaEngine = $("#schemaEngine").find(":selected").text();
    var location = "/schemaVisualize?" +
      schemaPart + "&" +
      "schemaFormat=" + encodeURIComponent(schemaFormat) + "&" +
      "schemaEngine=" + encodeURIComponent(schemaEngine);
    var href = urlShaclex + location
    console.log("NewHRef: " + href)
    window.location.assign(href) ;
  });

});