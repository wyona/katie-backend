package com.wyona.katie.models.discord;

public class DiscordEvent {

    private String channelRequestId;
    private String domainId;
    private String guildId;
    private String channelId;
    private String msgId;

    private String threadChannelId;

    /**
     *
     */
    public DiscordEvent() {
    }

    /**
     *
     */
    public void setChannelRequestId(String channelRequestId) {
        this.channelRequestId = channelRequestId;
    }

    /**
     *
     */
    public String getChannelRequestId() {
        return channelRequestId;
    }

    /**
     * @param msgId Message Id, e.g. "1016594091506679868"
     */
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /**
     * @return message Id, e.g. "1016594091506679868"
     */
    public String getMsgId() {
        return msgId;
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

    /**
     *
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     *
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     *
     */
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    /**
     *
     */
    public String getGuildId() {
        return this.guildId;
    }
}
