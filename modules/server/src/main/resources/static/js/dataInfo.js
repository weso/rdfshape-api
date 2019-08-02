var codeMirrorData ;

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
        case "HTML": mode = "htmlembedded" ;
            break ;
    }
    element.setOption("mode",mode);
}

function changeTheme(theme) {
    codeMirrorData.setOption("theme",theme);
}

function showDot(dot, idName) {
    $(idName).append("<div id='graph'></div>");
    var format=$("#SelectFormat option:selected").text();
    console.log("Selected format " + format);
    var engine=$("#SelectEngine option:selected").text();
    console.log("Selected engine " + engine);
    $("#graph").empty();
    var v = new Viz();
    var opts = { engine: engine };
    switch (format) {
       case "SVG":
           console.log("Rendering dot...")
           console.log(dot)
           v.renderSVGElement(dot, opts).then(function(svg) {
           console.log(svg);
           var graph = $("<svg/>").append(svg);
           $("#graph").append(graph);
          });
          break;
       case "PNG":
         v.renderImageElement(dot,opts).then(function(element) {
          $("#graph").append(element);
         });
         break;
    }
    v.render;
}

$(document).ready(function(){

function showResult(result) {
    result = $("#resultDiv").data("result");
    if(result) {
        console.log("Result.nodesPrefixMap: " + JSON.stringify(result.nodesPrefixMap));
        var tableHead = "<thead><tr>" +
                "<th data-sortable=\"true\">Name</th>" +
                "<th data-sortable=\"true\">Value</th>" +
                "</tr></thead>";
        var tableBody = '';
        tableBody += "<tr><td>Number of triples</td><td>" + result.statements + "</td></tr>";
        $("#resultDiv").append("<table data-toggle=\"table\" data-sort-order=\"desc\" data-sort-name=\"node\">" +
                tableHead +
                tableBody +
                "</table>");
        var pre = $("<pre/>").text(JSON.stringify(result,undefined,2));
        var details = $("<details/>").append(pre);
        $("#resultDiv").append(details);
        showDot(result.dot, "#resultDiv");
        $("#SelectFormat").change(function() {
            showDot(result.dot, "#resultDiv");
        });
        $("#SelectEngine").change(function() {
            showDot(result.dot, "#resultDiv");
        });
    }
}

  var urlShaclex = getHost();
    // When loading document get result from data-result attribute and show it
    var result = $("#resultDiv").data("result");
    showResult(result);

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
  var location = "/dataInfo?" +
      dataPart + "&" +
      "dataFormat=" + encodeURIComponent(dataFormat) +
      "&inference=" + encodeURIComponent(inference) ;
    var href = urlShaclex + location;
    console.log("NewHRef: " + href);
    window.location.assign(href) ;
  });

});
