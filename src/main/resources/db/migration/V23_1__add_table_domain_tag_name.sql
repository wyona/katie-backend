/* Mapping between domain tag name and domain id */
create table DOMAIN_TAG_NAME (
    TAG_NAME varchar(100) not null,
    DOMAIN_ID varchar(100) not null
);
insert into DOMAIN_TAG_NAME (TAG_NAME, DOMAIN_ID) values ('root','ROOT');
