# Database schema for courses
 
# --- !Ups


CREATE SEQUENCE course_id_seq start with 1000;

CREATE TABLE course (
    id 				bigint NOT NULL DEFAULT nextval('course_id_seq'),
    code 			varchar(20) not null,
    name 			varchar(200) not null,
    starts			varchar(100),
    ends			varchar(100),
    constraint 		pk_course primary key (id)
);


# --- !Downs

DROP TABLE if exists course;

DROP SEQUENCE if exists course_id_seq;
