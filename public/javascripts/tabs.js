$(function() {
	var datas=["#byUri_data","#byFile_data","#byInput_data","#byEndpoint_data","#byDereference_data"];
	
	$( "#tabs-data" ).tabs({
    active: datas.indexOf($("#data").attr('value')),
    activate: function( event, ui ) { 
    	$("#data").attr('value',ui.newPanel.selector) 
    } 
    });

	var schemas=["#no_schema","#schema"]

	$( "#tabs-schema" ).tabs({
	active:schemas.indexOf($("#schema").attr('value')),
	activate: function( event, ui ) { 
    	$("#schema").attr('value',ui.newPanel.selector)} 
    });

	var input_schemas= ["#byUri_schema","#byFile_schema","#byInput_schema"];
	
	$( "#tabs-input-schema" ).tabs({
		active:schemas.indexOf($("#input-schema").attr('value')),
		activate: function( event, ui ) { 
	    	$("#input-schema").attr('value',ui.newPanel.selector)} 
	    });

	/* iri */
	var iris=["#noIri","#iri"];
	
	$( "#tabsIriNoIri" ).tabs({ 
	active: iris.indexOf($("#iriNoIri").attr('value')),	
	activate: function( event, ui ) {
		$("#iriNoIri").attr('value',ui.newPanel.selector) } 
    });

});
