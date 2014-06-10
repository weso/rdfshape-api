$(function() {
	var rdfs=["#byUri_rdf","#byFile_rdf","#byInput_rdf","#byEndpoint_rdf","#byDeref_rdf"];
	
	$( "#tabs-rdf" ).tabs({
    active: rdfs.indexOf($("#rdf").attr('value')),
    activate: function( event, ui ) { 
    	$("#rdf").attr('value',ui.newPanel.selector) 
    } 
    });

	var schemas=["#no_schema","#byUri_schema","#byFile_schema","#byInput_schema"];

	$( "#tabs-schema" ).tabs({
	active:schemas.indexOf($("#schema").attr('value')),
	activate: function( event, ui ) { 
    	$("#schema").attr('value',ui.newPanel.selector)} 
    });

	/* byUri iri */
	var byUri_iris=["#noIri_byUri","#iri_byUri"];
	
	$( "#tabsIriNoIri_byUri" ).tabs({ 
	active:byUri_iris.indexOf($("#iri").attr('value')),	
	activate: function( event, ui ) {
		$("#iri").attr('value',ui.newPanel.selector) } 
    });

	/* byFile iri */
	var byFile_iris=["#noIri_byFile","#iri_byFile"];

	$( "#tabsIriNoIri_byFile" ).tabs({
	active:byFile_iris.indexOf($("#iri").attr('value')),	
	activate: function( event, ui ) { 
    	$("#iri").attr('value',ui.newPanel.selector) } 
    });

	/* byInput iri */
	var byInput_iris=["#noIri_byInput","#iri_byInput"];

	$( "#tabsIriNoIri_byInput" ).tabs({ 
	active:byInput_iris.indexOf($("#iri").attr('value')),
	activate: function( event, ui ) { 
    	$("#iri").attr('value',ui.newPanel.selector) } 
    });

});
