# Database schema for languages
 
# --- !Ups


CREATE SEQUENCE schema_id_seq start with 1000;

CREATE TABLE schema (
    id 				bigint NOT NULL DEFAULT nextval('student_id_seq'),
    name 			varchar(100) not null,
    context 		text,
    format			varchar(100) not null,
    constraint 		pk_schema primary key (id)
);


# --- !Downs

DROP TABLE if exists schema;

DROP SEQUENCE if exists schema_id_seq;
