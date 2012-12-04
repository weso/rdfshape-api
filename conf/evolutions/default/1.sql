# Tasks schema
 
# --- !Ups

CREATE SEQUENCE task_id_seq;
CREATE TABLE task (
    id integer NOT NULL DEFAULT nextval('task_id_seq'),
    label varchar(255)
);
 
# --- !Downs
 
DROP TABLE task;
DROP SEQUENCE task_id_seq;