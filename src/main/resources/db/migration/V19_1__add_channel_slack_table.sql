/* Slack */
create table CHANNEL_SLACK (
    CHANNEL_REQUEST_UUID varchar(100) not null,
    DOMAIN_ID varchar(100),
    SLACK_CHANNEL_ID varchar(250) not null
);
