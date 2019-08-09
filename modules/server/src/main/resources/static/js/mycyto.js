var cy = cytoscape({

  container: document.getElementById('cy'), // container to render in
  elements: [ 
    // Nodes 
    { data: { id: 'a', label: ':alice', type: 'uri' } },
    { data: { id: 'b', label: ':carol', type: 'uri' } },
    { data: { id: 'c', label: ':carol', type: 'uri' } },
    { data: { id: 'd', label: ':dave', type: 'uri' } },
    { data: { id: 'aliceStr', label: '\"Alice\"', type: 'literal' } },

    // Arcs
    { data: { source: 'a', target: 'b', label: ':knows' } },
    { data: { source: 'a', target: 'd', label: ':knows' } },
    { data: { source: 'a', target: 'c', label: ':knows' } },
    { data: { source: 'a', target: 'aliceStr', label: 'rdfs:label' } },
    { data: { source: 'c', target: 'c', label: ':knows' } }
  ],
  style: [ // the stylesheet for the graph
    {
      selector: 'node',
      style: {
        'background-color': 'blue',
        'label': 'data(label)'
      }
    },

    {
      selector: 'edge',
      style: {
        'width': 3,
        'line-color': '#ccc',
        'target-arrow-color': '#ccc',
        'target-arrow-shape': 'triangle',
        'label': 'data(label)',
        'curve-style': 'bezier',
        'control-point-step-size': 40 
      }
    }
  ],
/*  layout: {
    name: 'grid',
    rows: 1
  } */
 layout: {
    name: 'random',
    animate: true
  } 

});
