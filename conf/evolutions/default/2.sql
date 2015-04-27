# Database schema for languages
 
# --- !Ups


CREATE SEQUENCE student_id_seq start with 1000;

CREATE TABLE student (
    id 				bigint NOT NULL DEFAULT nextval('student_id_seq'),
    dni 			varchar(100) not null,
    firstName 		varchar(255) not null,
    lastName 		varchar(255) not null,
    email			varchar(255) not null,
    lat 		    double,
    long			double,
    constraint 		pk_student primary key (id)
);


# --- !Downs

DROP TABLE if exists student;

DROP SEQUENCE if exists student_id_seq;
