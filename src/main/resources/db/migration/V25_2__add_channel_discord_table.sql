/* Discord requests */
create table CHANNEL_DISCORD (
    CHANNEL_REQUEST_UUID varchar(100) not null,
    DOMAIN_ID varchar(100) not null,
    DISCORD_GUILD_ID varchar(50) not null,
    DISCORD_CHANNEL_ID varchar(50) not null,
    DISCORD_MSG_ID varchar(50) not null,
    DISCORD_THREAD_CHANNEL_ID varchar(50)
);
