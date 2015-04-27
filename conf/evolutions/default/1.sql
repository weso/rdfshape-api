# Database schema for courses
 
# --- !Ups


CREATE SEQUENCE data_id_seq start with 1000;

CREATE TABLE data (
    id 				bigint NOT NULL DEFAULT nextval('data_id_seq'),
    name 			varchar(200) not null,
    content			text not null,
    format			varchar(200) not null,
    constraint 		pk_data primary key (id)
);


# --- !Downs

DROP TABLE if exists data;

DROP SEQUENCE if exists data_id_seq;
