/* Mapping between MS Teams teams and Katie contexts/domains */
create table MS_TEAM_KATIE_DOMAIN (
    MS_TEAM_ID varchar(100) not null,
    KATIE_DOMAIN_ID varchar(100) not null,
    TIMESTAMP_CREATED varchar(100) not null
);
