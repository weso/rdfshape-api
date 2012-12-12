# Database schema for translations
 
# --- !Ups


CREATE SEQUENCE trans_id_seq start with 1000;

CREATE TABLE translation (
    id 				bigint NOT NULL DEFAULT nextval('trans_id_seq') not null,
    iriId 			bigint not null,
    langId 			bigint not null,
    transLabel 		varchar(5000) not null,
    votes 			bigint,
    constraint 		pk_trans primary key (id)
);


alter table translation add constraint fk_iri_1 foreign key (iriId) references iri (id) on delete restrict on update restrict;
alter table translation add constraint fk_lang_1 foreign key (langId) references language (id) on delete restrict on update restrict;

create index ix_trans_iri_1 on translation (iriId);
create index ix_trans_lang_1 on translation (langId);

insert into translation (id,iriId,langId,transLabel,votes) values (1,1,1,'Person',2);
insert into translation (id,iriId,langId,transLabel,votes) values (2,1,2,'Persona',2);
insert into translation (id,iriId,langId,transLabel,votes) values (3,2,1,'Orgnization',2);
insert into translation (id,iriId,langId,transLabel,votes) values (4,2,2,'Organzación',2);
insert into translation (id,iriId,langId,transLabel,votes) values (5,3,1,'Project',2);
insert into translation (id,iriId,langId,transLabel,votes) values (6,3,2,'Proyecto',2);

 
# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE if exists translation;

DROP SEQUENCE if exists trans_id_seq;