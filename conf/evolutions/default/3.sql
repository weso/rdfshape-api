# Langs schema
 
# --- !Ups

CREATE SEQUENCE lang_id_seq;
CREATE TABLE lang (
    id integer NOT NULL DEFAULT nextval('lang_id_seq'),
    langCode varchar(100),
    langName varchar(255)
);
 
# --- !Downs
 
DROP TABLE lang;
DROP SEQUENCE lang_id_seq;