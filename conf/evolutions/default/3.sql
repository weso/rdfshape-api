# Database schema for translations
 
# --- !Ups


CREATE SEQUENCE enrolment_id_seq start with 1000;

CREATE TABLE enrolment (
    id 				bigint NOT NULL DEFAULT nextval('enrolment_id_seq') not null,
    courseId 		bigint not null,
    studentId 		bigint not null,
    grade 			double,
    constraint 		pk_enrolment primary key (id)
);


alter table enrolment add constraint fk_course_1 foreign key (courseId) references course (id) on delete restrict on update restrict;
alter table enrolment add constraint fk_srudent_1 foreign key (studentId) references student (id) on delete restrict on update restrict;

create index ix_course_iri_1 on enrolment (courseId);
create index ix_student_lang_1 on enrolment (studentId);

 
# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE if exists enrolment;

DROP SEQUENCE if exists enrolment_id_seq;