/* Generic analytics table */
create table ANALYTICS (
    UUID varchar(100) not null,
    DOMAIN_ID varchar(100) not null,
    EVENT_TYPE varchar(100) not null,
    TIMESTAMP varchar(100) not null,
    LANGUAGE varchar(2),
    AGENT varchar(100),
    REMOTE_ADDRESS varchar(100)
);
