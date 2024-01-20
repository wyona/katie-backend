/* Mapping between Discord guilds / channels and Katie contexts/domains */
create table DISCORD_KATIE_DOMAIN (
    DISCORD_GUILD_ID varchar(100) not null,
    DISCORD_CHANNEL_ID varchar(100) not null,
    KATIE_DOMAIN_ID varchar(100) not null,
    TIMESTAMP_CREATED varchar(100) not null,
    STATUS varchar(25) not null,
    APPROVAL_TOKEN varchar(500)
);
