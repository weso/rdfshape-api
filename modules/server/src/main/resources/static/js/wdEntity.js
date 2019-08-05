var codeMirrorEntity ;
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

 $("#permalink").click(function(e) {
  e.preventDefault();
  console.log("click on permalink...");
  var entity = codeMirrorEntity.getValue();
  var entityPart = "entity=" + encodeURIComponent(entity);
  var location = "/wikidata/entity?" + entityPart ;
  var href = urlShaclex + location;
  console.log("NewHRef: " + href);
  window.location.assign(href) ;
});

});
