# Database schema for IRIs
 
# --- !Ups

set ignorecase true;

CREATE SEQUENCE iri_id_seq start with 1000;

CREATE TABLE iri (
    id 				bigint NOT NULL DEFAULT nextval('iri_id_seq'),
    iriName 		varchar(1000) not null,
    constraint 		pk_iri primary key (id)
);

insert into iri (id,iriName) values (1,'http://xmlns.com/foaf/0.1/Person');
insert into iri (id,iriName) values (2,'http://xmlns.com/foaf/0.1/Organization');
insert into iri (id,iriName) values (3,'http://xmlns.com/foaf/0.1/Project');

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE if exists iri;
