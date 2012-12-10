db.iris.drop();
db.iris.insert({_id: "http://xmlns.com/foaf/0.1/Project", prefix: "foaf", localName: "Project", 
	            labels: [{ language: "en", label: "Project"
	                           },
	                           { language: "es", label: "Proyecto", votes: [ { user: "pepe", value: 1  },
	                                                                         { user: "unreg", value: 1 }
	                                                                       ]} 
	                          ] 
	             });

db.iris.insert( { _id: "http://xmlns.com/foaf/0.1/Person", prefix: "foaf", localName: "Person" ,
				  labels: [{ language: "en", label: "Person" }
	                            ,
	                             { language: "es", label: "Persona", votes: [ { user: "kiko", value: 1  },
	                                                                          { user: "unreg", value: 1 }
	                                                                        ]} 
	                          ]
	          } );

db.iris.insert({ _id: "http://xmlns.com/foaf/0.1/Organization", prefix: "foaf", localName: "Organization",
				 labels: [{ language: "en", label: "Organization"
	                           },
	                           { language: "es", label: "Organización", votes: [ { user: "kiko", value: 1  },
	                                                                             { user: "unreg", value: 1 }
	                                                                           ]} 
	                          ] 
				 });

db.iris.insert({ _id: "http://www.w3.org/2002/07/owl#", prefix: "owl" ,
				 labels: [{ language: "en", label: "OWL Namespace" },
	                               { language: "es", label: "Namespace OWL", votes: [ { user: "unreg", value: 1 }]} 
	                              ]  
				});

db.iris.insert({ _id: "http://www.w3.org/2002/07/owl#Thing", prefix: "owl", localName: "Thing" ,
				 labels: [{ language: "en", label: "Thing" },
	                      { language: "es", label: "Cualquier cosa", votes: [ { user: "unreg", value: 1 }]} 
	                     ]  
				}); 

db.iris.insert( { _id: "http://www.lexvo.org/id/iso639-3/eng", prefix: "lexvo639-3", localName: "eng" ,
				  labels: [{ language: "en", label: "English" },
	                      { language: "es", label: "Inglés", votes: [ { user: "unreg", value: 1 }]} 
	                     ]  
                 });

db.iris.insert( { _id: "http://www.lexvo.org/id/iso639-3/spa", prefix: "lexvo639-3", localName: "spa" ,
				  labels: [{ language: "en", label: "Spanish" },
	                       { language: "es", label: "Español", votes: [ { user: "unreg", value: 1 }]} 
	                     ]  
				});

db.languages.drop();
db.languages.insert({ _id: "en", iri: "http://www.lexvo.org/id/iso639-3/eng"});
db.languages.insert({ _id: "es", iri: "http://www.lexvo.org/id/iso639-3/spa"});

db.users.drop();
db.users.insert({_id : "labra", name: "Jose Labra", groupRef: 1});
db.users.insert({_id : "pepe", name: "pp", groupRef: 2});
db.users.insert({_id : "unregistered", name: "kiko", groupRef: 3});


