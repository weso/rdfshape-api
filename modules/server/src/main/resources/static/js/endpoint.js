var codeMirrorEndpoint ;
var codeMirrorNode ;
var codeMirrorData ; // To show results


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

$(document).ready(function(){

 function showResult(result) {
    result = $("#resultDiv").data("result");
    if(result) {
        if (result.uri) {
            console.log("Uri: " + result.uri);
            $("#loadingButton").show();
            $.get(result.uri, function(data) {
                console.log("Data received from " + result.uri);
                $("#loadingButton").hide();
                var resultCode = $("<pre/>").attr("id","result").text(data);
                $("#resultDiv").append(resultCode);
            });
        }
        if (result.error) {
            var msg = $("<p/>").text(result.error);
            $("#resultDiv").append(msg);
        }
        var pre = $("<pre/>").text(JSON.stringify(result,undefined,2));
        var details = $("<details/>").append(pre);
        $("#resultDiv").append(details);

    if (result.dot) {
        showDot(result.dot, "#resultDiv");
        $("#SelectFormat").change(function() {
            showDot(result.dot, "#resultDiv");
        });
        $("#SelectEngine").change(function() {
            showDot(result.dot, "#resultDiv");
        });
     }
    }
 }

 var urlShaclex = getHost();
 // When loading document get result from data-result attribute and show it
 var result = $("#resultDiv").data("result");
 showResult(result);

 $("#permalink").click(function(e) {
  e.preventDefault();
  console.log("click on permalink...");
  var endpoint = codeMirrorEndpoint.getValue();
  var node = codeMirrorNode.getValue();
  var endpointPart = "endpoint=" + encodeURIComponent(endpoint);
  var nodePart = "node=" + encodeURIComponent(node);
  var location = "/endpoint?" +
      endpointPart + "&" +
      nodePart ;
  var href = urlShaclex + location;
  console.log("NewHRef: " + href);
  window.location.assign(href) ;
});

});
