# Translations schema
 
# --- !Ups

CREATE SEQUENCE trans_id_seq;
CREATE TABLE trans (
    id integer NOT NULL DEFAULT nextval('trans_id_seq'),
    iriId integer,
    langId integer,
    transLabel varchar(5000),
    votes integer
);
 
# --- !Downs
 
DROP TABLE trans;
DROP SEQUENCE trans_id_seq;