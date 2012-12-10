# Database schema for translations
 
# --- !Ups

set ignorecase true;

CREATE SEQUENCE trans_id_seq start with 1000;

CREATE TABLE translation (
    id 				bigint NOT NULL DEFAULT nextval('trans_id_seq') not null,
    iriId 			bigint not null,
    langId 			bigint not null,
    transLabel 		varchar(5000) not null,
    votes 			integer,
    constraint 		pk_trans primary key (id)
);


alter table translation add constraint fk_iri_1 foreign key (iriId) references iri (id) on delete restrict on update restrict;
alter table translation add constraint fk_lang_1 foreign key (langId) references language (id) on delete restrict on update restrict;

create index ix_trans_iri_1 on translation (iriId);
create index ix_trans_lang_1 on translation (langId);
 
# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE if exists translation;

DROP SEQUENCE if exists trans_id_seq;