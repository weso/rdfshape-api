var codeMirrorData ;
var codeMirrorSchema ;
var codeMirrorNodeSelector ;

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
    codeMirrorData.setOption("theme",theme);
    codeMirrorSchema.setOption("theme",theme);
    codeMirrorNodeSelector.setOption("theme",theme);
}

$(document).ready(function(){

function showResult(result) {
    result = $("#resultDiv").data("result");
    if(result) {
        var textArea = $("<textarea/>").attr("id","schemaAreaId").text(result.inferedShape);
        $("#resultDiv").append(textArea);
        var pre = $("<pre/>").text(JSON.stringify(result,undefined,2));
        var details = $("<details/>").append(pre);
        $("#resultDiv").append(details);
        codeMirrorSchema = CodeMirror.fromTextArea(schemaAreaId, {
            lineNumbers: true,
            mode: "shex",
            viewportMargin: Infinity,
            matchBrackets: true
        });
    }
}

  var urlShaclex = getHost();
    // When loading document get result from data-result attribute and show it
  var result = $("#resultDiv").data("result");
  showResult(result);

  var nodeSelector = document.getElementById("nodeSelector");
  if (nodeSelector) {
    codeMirrorNodeSelector = CodeMirror.fromTextArea(nodeSelector, {
            lineNumbers: true,
            mode: "shex",
            viewportMargin: Infinity,
            matchBrackets: true
     });
    codeMirrorNodeSelector.setSize(null,"2em");
  }


    var rdfData = document.getElementById("rdfData");
  if (rdfData) {
        codeMirrorData = CodeMirror.fromTextArea(rdfData, {
            lineNumbers: true,
            mode: "turtle",
            viewportMargin: Infinity,
            matchBrackets: true
        });
  }

 $('.dataPanel a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
   var name = $(e.target).attr("href");
   console.log("New tab: " + name); // newly activated tab
   $('#rdfDataActiveTab').val(name);
 });



 $("#permalink").click(function(e) {
  e.preventDefault();
  console.log("click on permalink...");
  var data = codeMirrorData.getValue();
  var dataActiveTab = $("#rdfDataActiveTab").attr("value");
  var dataPart = "";
  var dataFormat = "";
  switch (dataActiveTab) {
      case "#dataTextArea":
          dataFormat = $("#dataFormatTextArea").find(":selected").text();
          dataPart = "data=" + encodeURIComponent(data) ;
          break;
      case "#dataFile":
          dataFormat = $("#dataFormatFile").find(":selected").text();
          dataPart = "data=" + encodeURIComponent(data) ;
          break;
      case "#dataUrl":
          dataFormat = $("#dataFormatUrl").find(":selected").text();
          var dataURL = $("#dataURL").val();
          dataPart = "dataURL=" + encodeURIComponent(dataURL) ;
          break;
      case "#dataEndpoint":
          var endpoint = $("#inputDataEndpoint").val();
          dataPart = "endpoint=" + encodeURIComponent(endpoint) ;
          break;
      default:
          console.log("Unknown value of dataActiveTab:" + dataActiveTab);
          dataFormat = $("#dataFormatTextArea").find(":selected").text();
          dataPart = "data=" + encodeURIComponent(data) ;
          break;
     }
  var inference = $("#inference").find(":selected").text();
  var schemaEngine = $("#schemaEngine").find(":selected").text();
  var schemaFormat = $("#schemaFormatTextArea").find(":selected").text();
  var nodeSelector = $("#nodeSelector").val();
  var location = "/shapeInfer?" +
      dataPart +
      "&dataFormat=" + encodeURIComponent(dataFormat) +
      "&inference=" + encodeURIComponent(inference) +
      "&activeDataTab=" + encodeURIComponent(dataActiveTab) +
      "&schemaEngine=" + encodeURIComponent(schemaEngine) +
      "&schemaFormat=" + encodeURIComponent(schemaFormat) +
      "&nodeSelector=" + encodeURIComponent(nodeSelector)
     ;
    var href = urlShaclex + location;
    console.log("NewHRef: " + href);
    window.location.assign(href) ;
  });

});
