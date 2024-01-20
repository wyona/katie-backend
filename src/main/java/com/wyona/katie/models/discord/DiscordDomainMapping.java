package com.wyona.katie.models.discord;


import lombok.extern.slf4j.Slf4j;

/**
 * Discord guild/channel Katie domain mapping
 */
@Slf4j
public class DiscordDomainMapping {

    private String channelId;
    private String guildId;
    private String domainId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public DiscordDomainMapping() {
    }

    /**
     * @param channelId Channel Id, e.g. '996391611275694131'
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * @return channel Id, e.g. '996391611275694131'
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * Set guild Id, e.g. '996391257549053952'
     */
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    /**
     * Get guild Id, e.g. '996391257549053952'
     */
    public String getGuildId() {
        return guildId;
    }

    /**
     *
     */
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public String getDomainId() {
        return domainId;
    }
}
