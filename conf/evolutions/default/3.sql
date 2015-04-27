# Database schema for translations
 
# --- !Ups


CREATE SEQUENCE validation_id_seq start with 1000;

CREATE TABLE validation (
    id 				bigint NOT NULL DEFAULT nextval('validation_id_seq') not null,
    dataId 			bigint not null,
    schemaId 		bigint not null,
    constraint 		pk_validation primary key (id)
);


alter table validation add constraint fk_data_1 foreign key (dataId) references data (id) on delete restrict on update restrict;
alter table validation add constraint fk_schema_1 foreign key (schemaId) references schema (id) on delete restrict on update restrict;

create index ix_data_iri_1 on validation (dataId);
create index ix_schema_lang_1 on validation (schemaId);

 
# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE if exists validation;

DROP SEQUENCE if exists validation_id_seq;