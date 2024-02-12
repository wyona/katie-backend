package com.wyona.katie.models.slack;

/**
 *
 */
public class AccessCredentials {

    String teamId;
    String teamName;

    String channelId;
    String channelName;

    String accessToken;

    String botUserId;

    /**
     * @param botUserId User Id of Katie bot, which is generated per Slack team/workspace
     */
    public AccessCredentials(String teamId, String teamName, String channelId, String channelName, String accessToken, String botUserId) {
        this.teamId = teamId;
        this.teamName = teamName;

        this.channelId = channelId;
        this.channelName = channelName;

        this.accessToken = accessToken;

        this.botUserId = botUserId;
    }

    /**
     *
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     *
     */
    public String getTeamName() {
        return teamName;
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
    public String getChannelName() {
        return channelName;
    }

    /**
     *
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Get Katie bot user Id
     */
    public String getBotUserId() {
        return botUserId;
    }
}
