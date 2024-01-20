/* Mapping between Slack team and bearer token and Katie user Id */
create table SLACK_TEAM_TOKEN_USERID (
    SLACK_TEAM_ID varchar(100) not null,
    BEARER_TOKEN varchar(100) not null,
    USER_ID varchar(100) not null
);
alter table SLACK_TEAM_KATIE_DOMAIN drop BEARER_TOKEN;
