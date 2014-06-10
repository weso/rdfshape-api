$(function() {
    $( "#tabs-rdf" ).tabs({ activate: function( event, ui ) { 
    	$("#rdf").attr('value',ui.newPanel.selector) } 
    });
    $( "#tabs-schema" ).tabs({ activate: function( event, ui ) { 
    	$("#schema").attr('value',ui.newPanel.selector)} 
    });
    $( "#tabsIriNoIri_byUri" ).tabs({ activate: function( event, ui ) { 
    	$("#iri").attr('value',ui.newPanel.selector) } 
    });
    $( "#tabsIriNoIri_byFile" ).tabs({ activate: function( event, ui ) { 
    	$("#iri").attr('value',ui.newPanel.selector) } 
    });
    $( "#tabsIriNoIri_byInput" ).tabs({ activate: function( event, ui ) { 
    	$("#iri").attr('value',ui.newPanel.selector) } 
    });
});
