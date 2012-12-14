insert into iri (id,iriName) values (1,'http://xmlns.com/foaf/0.1/Person');
insert into iri (id,iriName) values (2,'http://xmlns.com/foaf/0.1/Organization');
insert into iri (id,iriName) values (3,'http://xmlns.com/foaf/0.1/Project');

insert into language (id,langCode,langName) values (1,'en', 'English');
insert into language (id,langCode,langName) values (2,'es', 'Spanish');

insert into translation (id,iriId,langId,transLabel,votes) values (1,1,1,'Person',2);
insert into translation (id,iriId,langId,transLabel,votes) values (2,1,2,'Persona',2);
insert into translation (id,iriId,langId,transLabel,votes) values (3,2,1,'Orgnization',2);
insert into translation (id,iriId,langId,transLabel,votes) values (4,2,2,'Organzación',2);
insert into translation (id,iriId,langId,transLabel,votes) values (5,3,1,'Project',2);
insert into translation (id,iriId,langId,transLabel,votes) values (6,3,2,'Proyecto',2);

