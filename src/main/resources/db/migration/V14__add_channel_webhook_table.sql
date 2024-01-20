/* Webhook echo data */
create table CHANNEL_WEBHOOK (
    UUID varchar(100) not null,
    DOMAIN_ID varchar(100),
    ECHO_DATA varchar(200) not null
);
