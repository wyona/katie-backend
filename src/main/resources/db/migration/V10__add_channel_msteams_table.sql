/* Connection values re Channel MS Teams */
create table CHANNEL_MS_TEAMS (
    UUID_RESUBMITTED_QUESTION varchar(100) not null,
    DOMAIN_ID varchar(100),
    SERVICE_URL varchar(100) not null,
    CONVERSATION_ID varchar(200) not null,
    CONVERSATION_NAME varchar(200),
    MESSAGE_ID varchar(200) not null,
    KATIE_BOT_ID varchar(200) not null,
    KATIE_BOT_NAME varchar(200),
    MS_TEAMS_USER_ID varchar(200) not null,
    MS_TEAMS_USER_NAME varchar(200)
);
