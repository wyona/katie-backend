/* Mapping between Matrix rooms and Katie domains */
create table MATRIX_KATIE_DOMAIN (
    MATRIX_ROOM_ID varchar(100) not null,
    KATIE_DOMAIN_ID varchar(100) not null,
    TIMESTAMP_CREATED varchar(100) not null
);
