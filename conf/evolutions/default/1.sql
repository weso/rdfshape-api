# IRIs schema
 
# --- !Ups

CREATE SEQUENCE iri_id_seq;
CREATE TABLE iri (
    id integer NOT NULL DEFAULT nextval('iri_id_seq'),
    iriName varchar(255)
);
 
# --- !Downs
 
DROP TABLE iri;
DROP SEQUENCE iri_id_seq;