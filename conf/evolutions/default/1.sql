# Database schema for IRIs
 
# --- !Ups


CREATE SEQUENCE iri_id_seq start with 1000;

CREATE TABLE iri (
    id 				bigint NOT NULL DEFAULT nextval('iri_id_seq'),
    iriName 		varchar(1000) not null,
    constraint 		pk_iri primary key (id)
);


# --- !Downs

DROP TABLE if exists iri;

DROP SEQUENCE if exists iri_id_seq;
