/* Database tables */

/* Questions which have been resubmitted to expert, because answer by Katie was not helpful */
create table RESUBMITTED_QUESTION (
    UUID varchar(100) not null,
    DOMAIN_ID varchar(100),
    QUESTION varchar(500) not null,
    EMAIL varchar(100),
    FCM_TOKEN varchar(250),
    SLACK_CHANNEL_ID varchar(250),
    ANSWER_LINK_TYPE varchar(10),
    STATUS varchar(100) not null,
    TRAINED boolean,
    REMOTE_ADDRESS varchar(100) not null,
    TIMESTAMP_RESUBMITTED varchar(100) not null,
    ANSWER varchar(500),
    ANSWER_CLIENT_SIDE_ENCRYPTED_ALGORITHM varchar(100),
    OWNERSHIP varchar(100),
    RESPONDENT varchar(100),
    RATING varchar(10),
    FEEDBACK varchar(250)
);

/* All questions submitted to Katie */
create table QUESTION (
    UUID varchar(100) not null,
    DOMAIN_ID varchar(100),
    QUESTION varchar(500) not null,
    REMOTE_ADDRESS varchar(100) not null,
    TIMESTAMP varchar(100) not null
);

/* Remember me tokens to stay signed in */
create table REMEMBERME (
    USER_ID varchar(100) not null,
    TOKEN varchar(256) not null,
    EXPIRY_DATE varchar(100) not null
);

/* Mapping between Slack teams and Katie contexts/domains */
create table SLACK_TEAM_KATIE_DOMAIN (
    SLACK_TEAM_ID varchar(100) not null,
    KATIE_DOMAIN_ID varchar(100) not null,
    BEARER_TOKEN varchar(100) not null,
    TIMESTAMP_CREATED varchar(100) not null
);
